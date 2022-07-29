package com.redhat.console.notifications.splunkintegration;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/* 

1) Adapt code to receive parameter from env variables
2) Add parameters to application.properties
3) Refact errorHandlers
4) Refact ingress
5) Refact return
6) Refact success


*/
@ApplicationScoped
public class BaseRoutes extends ErrorHandler {

  @ConfigProperty(name = "integrations.component.name")
  String COMPONENT_NAME;
  
  // Only accept/listen on these CloudEvent types
  public String CE_TYPE;
  
  // Logger Name for logs using Log EIP
  public String LOGGER_NAME;
  
  // Event incoming kafka brokers
  @ConfigProperty(name = "kafka.bootstrap.servers")
  String kafkaBrokers;
  
  // Event incoming Kafka topic
  @ConfigProperty(name = "kafka.ingress.topic")
  String kafkaIngressTopic;
  
  // Event incoming kafka group id
  @ConfigProperty(name = "kafka.ingress.group.id")
  String kafkaIngressGroupId;
  
  // Event return Kafka topic
  @ConfigProperty(name = "kafka.return.topic")
  String kafkaReturnTopic;
  
  // Event return kafka group id
  @ConfigProperty(name = "kafka.return.group.id")
  String kafkaReturnGroupId;
  
  // The return type
  public static final String RETURN_TYPE = "com.redhat.console.notifications.history";

  @Override
  public void configure() throws Exception {
    super.configure();

    CE_TYPE = "com.redhat.console.integration." + COMPONENT_NAME;
    LOGGER_NAME = "com.redhat.console.notification.toCamel." + COMPONENT_NAME;
    
    getContext().getGlobalOptions().put(Exchange.LOG_EIP_NAME, LOGGER_NAME);
    
    configureIngress(); // need to adapt
    configureReturn();
    configureSuccessHandler(); // reusable
  }

  private void configureIngress() throws Exception {
    from(kafka(kafkaIngressTopic).groupId(kafkaIngressGroupId))
        .routeId("ingress")
        // Decode CloudEvent
        .process(new CloudEventDecoder())
        // We check that this is our type.
        // Otherwise, we ignore the message there will be another component that takes
        // care
        .filter().simple("${header.ce-type} == '" + CE_TYPE + "'")
        // Log the parsed cloudevent message.
        .to(log("com.redhat.console.notifications.splunkintegration?level=DEBUG"))
        .to(direct("handler"))
        .end();
  }

  private void configureReturn() throws Exception {
    from(direct("return"))
        .routeId("return")
        .to(kafka(kafkaReturnTopic));
  }

  private void configureSuccessHandler() throws Exception {
    Processor ceEncoder = new CloudEventEncoder(COMPONENT_NAME, RETURN_TYPE);
    Processor resultTransformer = new ResultTransformer();
    // If Event was sent successfully, send success reply to return kafka
    from(direct("success"))
        .routeId("success")
        .log("Delivered event ${header.ce-id} (orgId ${header.orgId} account ${header.accountId})"
                     + " to ${header.targetUrl}")
        .setBody(simple("Success: Event ${header.ce-id} sent successfully"))
        .setHeader("outcome-fail", simple("false"))
        .process(resultTransformer)
        .marshal().json()
        .process(ceEncoder)
        .to(direct("return"));
  }

}
