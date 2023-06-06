package com.redhat.console.integrations;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.camel.Exchange;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import static com.redhat.console.integrations.OutgoingCloudEventBuilder.OUTCOME_EXCHANGE_PROPERTY;
import static com.redhat.console.integrations.OutgoingCloudEventBuilder.SUCCESSFUL_EXCHANGE_PROPERTY;
import static org.apache.camel.LoggingLevel.INFO;

@ApplicationScoped
public class MainRoutes extends IntegrationsRouteBuilder {

    public static final String LOGGER_NAME = "com.redhat.console.notification.toCamel." + COMPONENT_NAME;

    // Only accept/listen on these CloudEvent types
    public static final String CE_TYPE = "com.redhat.console.notification.toCamel." + COMPONENT_NAME;

    // Event incoming Kafka topic
    @ConfigProperty(name = "mp.messaging.kafka.ingress.topic")
    String kafkaIngressTopic;

    // Event incoming kafka group id
    @ConfigProperty(name = "kafka.ingress.group.id")
    String kafkaIngressGroupId;

    // Event return Kafka topic
    @ConfigProperty(name = "mp.messaging.kafka.return.topic")
    String kafkaReturnTopic;

    @Inject
    OutgoingCloudEventBuilder outgoingCloudEventBuilder;

    @Override
    public void configure() throws Exception {
        super.configure();

        getContext().getGlobalOptions().put(Exchange.LOG_EIP_NAME, LOGGER_NAME);

        configureIngress();
        configureReturn();
        configureSuccessHandler();
    }

    private void configureIngress() {
        from(kafka(kafkaIngressTopic).groupId(kafkaIngressGroupId))
                .routeId("ingress")
                .log(INFO, "Received ${body}") // TODO Temp, remove ASAP
                // Decode CloudEvent
                .process(new CloudEventDecoder())
                // We check that this is our type.
                // Otherwise, we ignore the message there will be another component that takes
                // care
                .filter().simple("${header.ce-type} == '" + CE_TYPE + "'")
                // Log the parsed cloudevent message.
                .to(log("com.redhat.console.integrations?level=DEBUG"))
                .to(direct("handler"))
                .end();
    }

    private void configureReturn() {
        from(direct("return"))
                .routeId("return")
                .to(kafka(kafkaReturnTopic));
    }

    private void configureSuccessHandler() {
        // If Event was sent successfully, send success reply to return kafka
        from(direct("success"))
                .routeId("success")
                .log("Delivered event ${header.ce-id} (orgId ${header.orgId} account ${header.accountId})"
                     + " to ${exchangeProperty.targetUrl}")
                .setProperty(OUTCOME_EXCHANGE_PROPERTY, simple("Event ${header.ce-id} sent successfully"))
                .setProperty(SUCCESSFUL_EXCHANGE_PROPERTY, constant(true))
                .process(outgoingCloudEventBuilder)
                .to(direct("return"));
    }

}
