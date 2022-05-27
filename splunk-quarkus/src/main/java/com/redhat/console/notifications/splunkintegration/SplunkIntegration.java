/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.console.notifications.splunkintegration;

import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.component.http.HttpClientConfigurer;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.http.common.HttpHeaderFilterStrategy;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * The main class that does the work setting up the Camel routes. Entry point for messages is below
 * 'from(kafka(kafkaIngressTopic))' Upon success/failure a message is returned to the kafkaReturnTopic topic.
 */

/*
 * We need to register some classes for reflection here, so that
 * native compilation can work if desired.
 */
@RegisterForReflection(targets = {
        Exception.class,
        HttpOperationFailedException.class,
        IOException.class
})
@ApplicationScoped
public class SplunkIntegration extends EndpointRouteBuilder {

    // The name of our component. Must be unique
    public static final String COMPONENT_NAME = "splunk";
    // Logger Name for logs using Log EIP
    public static final String LOGGER_NAME = "com.redhat.console.integration." + COMPONENT_NAME;

    private static final Config CONFIG = ConfigProvider.getConfig();
    // Only accept/listen on these CloudEvent types
    public static final String CE_TYPE = "com.redhat.console.notification.toCamel." + COMPONENT_NAME;
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

    class SplunkHttpHeaderStrategy extends HttpHeaderFilterStrategy {
        @Override
        protected void initialize() {
            setLowerCase(true);
            setFilterOnMatch​(false); // reverse filtering to only accept selected

            getInFilter().clear();
            getOutFilter().clear();
            getOutFilter().add("authorization");
        }
    }

    @Override
    public void configure() throws Exception {
        getContext().getGlobalOptions().put(Exchange.LOG_EIP_NAME, LOGGER_NAME);

        configureErrorHandler();
        configureTargetUrlInvalidHandler();
        configureIngress();
        configureIoFailed();
        configureHttpFailed();
        configureTargetUrlValidationFailed();
        configureReturn();
        configureSuccessHandler();
        configureHandler();

    }

    private void configureTargetUrlInvalidHandler() throws Exception {
        onException(IllegalArgumentException.class)
                .to(direct("targetUrlValidationFailed"))
                .handled(true);
    }

    private void configureErrorHandler() throws Exception {
        onException(IOException.class)
                .to(direct("ioFailed"))
                .handled(true);
        onException(HttpOperationFailedException.class)
                .to(direct("httpFailed"))
                .handled(true);
    }

    private void configureTargetUrlValidationFailed() throws Exception {
        Processor ceEncoder = new CloudEventEncoder(COMPONENT_NAME, RETURN_TYPE);
        Processor resultTransformer = new ResultTransformer();
        // The error handler when we receive a TargetUrlValidator failure
        from(direct("targetUrlValidationFailed"))
                .routeId("targetUrlValidationFailed")
                .log(LoggingLevel.ERROR, "IllegalArgumentException for event ${header.ce-id} (account ${header.accountId})"
                                         + " to ${header.targetUrl}: ${exception.message}")
                .log(LoggingLevel.DEBUG, "${exception.stacktrace}")
                .setBody(simple("${exception.message}"))
                .setHeader("outcome-fail", simple("true"))
                .process(resultTransformer)
                .marshal().json()
                .process(ceEncoder)
                .to(direct("return"));
    }

    private void configureIoFailed() throws Exception {
        Processor ceEncoder = new CloudEventEncoder(COMPONENT_NAME, RETURN_TYPE);
        Processor resultTransformer = new ResultTransformer();
        // The error handler found an IO Exception. We set the outcome to fail and then send to kafka
        from(direct("ioFailed"))
                .routeId("ioFailed")
                .log(LoggingLevel.ERROR, "IOFailure for event ${header.ce-id} (account ${header.accountId})"
                                         + " to ${header.targetUrl}: ${exception.message}")
                .log(LoggingLevel.DEBUG, "${exception.stacktrace}")
                .setBody(simple("${exception.message}"))
                .setHeader("outcome-fail", simple("true"))
                .process(resultTransformer)
                .marshal().json()
                .process(ceEncoder)
                .to(direct("return"));
    }

    private void configureHttpFailed() throws Exception {
        Processor ceEncoder = new CloudEventEncoder(COMPONENT_NAME, RETURN_TYPE);
        Processor resultTransformer = new ResultTransformer();
        // The error handler found an HTTP Exception. We set the outcome to fail and then send to kafka
        from(direct("httpFailed"))
                .routeId("httpFailed")
                .log(LoggingLevel.ERROR, "HTTPFailure for event ${header.ce-id} (account ${header.accountId})"
                                         + " to ${header.targetUrl}: ${exception.getStatusCode()}"
                                         + " ${exception.getStatusText()}: ${exception.message}")
                .log(LoggingLevel.DEBUG, "Response Body: ${exception.getResponseBody()}")
                .log(LoggingLevel.DEBUG, "Response Headers: ${exception.getResponseHeaders()}")
                .setBody(simple("${exception.message}"))
                .setHeader("outcome-fail", simple("true"))
                .process(resultTransformer)
                .marshal().json()
                .process(ceEncoder)
                .to(direct("return"));
    }

    private void configureIngress() throws Exception {
        from(kafka(kafkaIngressTopic).brokers(kafkaBrokers).groupId(kafkaIngressGroupId))
                .routeId("ingress")
                // Decode CloudEvent
                .process(new CloudEventDecoder())
                // We check that this is our type.
                // Otherwise, we ignore the message there will be another component that takes care
                .filter().simple("${header.ce-type} == '" + CE_TYPE + "'")
                // Log the parsed cloudevent message.
                .to(log("com.redhat.console.notifications.splunkintegration?level=DEBUG"))
                .to(direct("handler"))
                .end();
    }

    private void configureReturn() throws Exception {
        from(direct("return"))
                .routeId("return")
                .to(kafka(kafkaReturnTopic).brokers(kafkaBrokers));
    }

    private void configureSuccessHandler() throws Exception {
        Processor ceEncoder = new CloudEventEncoder(COMPONENT_NAME, RETURN_TYPE);
        Processor resultTransformer = new ResultTransformer();
        // If Event was sent successfully, send success reply to return kafka
        from(direct("success"))
                .routeId("success")
                .log("Delivered event ${header.ce-id} (account ${header.accountId}) to ${header.targetUrl}")
                .setBody(simple("Success: Event ${header.ce-id} sent successfully"))
                .setHeader("outcome-fail", simple("false"))
                .process(resultTransformer)
                .marshal().json()
                .process(ceEncoder)
                .to(direct("return"));
    }

    private void configureHandler() throws Exception {
        Processor eventPicker = new EventPicker();
        // Receive messages on internal enpoint (within the same JVM)
        // named "splunk".
        from(direct("handler"))
                .routeId("handler")
                // Remove headers of previous message,
                // specifically the ones that HTTP components use
                // to prevent passing the REST path to the HTTP producer.
                // Without this it would use path: /services/collector/raw/event
                // where the "/event" is the REST endpoint configured on previous
                // component.
                .removeHeaders("CamelHttp*")

                //Add headers useful for error reporting and metrics
                .setHeader("targetUrl", simple("${headers.metadata[url]}"))
                .setHeader("timeIn", simpleF("%d", System.currentTimeMillis()))

                //Set Authorization header
                .setHeader("Authorization", simpleF("Splunk %s", "${headers.metadata[X-Insight-Token]}"))

                // body is a JsonObject so converting to consumable object
                // for the http producer
                .marshal().json(JsonLibrary.Jackson)

                .setProperty("eventsCount", jsonpath("$.events.length()"))

                // loops over events in the original message
                .loop(exchangeProperty("eventsCount")).copy()
                // picks one Event from the original message
                .process(eventPicker)

                // Transform message to add splunk wrapper to the json
                .transform().simple("{\"source\": \"eventing\", \"sourcetype\": \"Insights event\", \"event\": ${body}}")

                // aggregate transformed messages and append them together
                // aggregate by "metadata" header as it contains data unique per target splunk instance
                .aggregate(header("metadata"), new EventAppender())
                .completionSize(exchangeProperty("eventsCount"))
                .process(new TargetUrlValidator()) // validate the TargetUrl to be a proper url

                // Redirect depending on http or https (different default ports) so that it goes to the default splunk port
                // Send the message to Splunk's HEC as a splunk formattted event.
                // It sends token via Basic Preemptive Authentication.
                // POST method is being used, set up explicitly
                // (see https://camel.apache.org/components/latest/http-component.html#_which_http_method_will_be_used).
                .setHeader(Exchange.HTTP_URI, header("targetUrl"))
                .setHeader(Exchange.HTTP_PATH, constant("/services/collector/event"))
                .choice()
                .when(simple("${header.targetUrl} startsWith 'http://'"))
                .to(http("dynamic")
                        .httpMethod("POST")
                        .headerFilterStrategy(new SplunkHttpHeaderStrategy())
                        .advanced()
                        .httpClientConfigurer(getClientConfigurer()))
                .endChoice()
                .otherwise()
                .to(https("dynamic")
                        .httpMethod("POST")
                        .headerFilterStrategy(new SplunkHttpHeaderStrategy())
                        .advanced()
                        .httpClientConfigurer(getClientConfigurer()))
                .endChoice()
                .end()
                .to(direct("success"));
    }

    protected HttpClientConfigurer getClientConfigurer() {
        return (clientBuilder) -> {
            // proactively evict expired connections from the connection pool using a background thread
            clientBuilder.evictExpiredConnections();

            // proactively evict idle connections from the connection pool after 5s using a background thread.
            // Arguments set maximum time persistent connections can stay idle while kept alive in the connection pool.
            // Connections whose inactivity period exceeds this value will get closed and evicted from the pool.
            clientBuilder.evictIdleConnections(5L, TimeUnit.SECONDS);
        };
    }
}
