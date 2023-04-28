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
package com.redhat.console.integrations.splunk;

import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.http.HttpClientConfigurer;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.http.common.HttpHeaderFilterStrategy;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.ProtocolException;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.console.integrations.IntegrationsRouteBuilder;
import com.redhat.console.integrations.TargetUrlValidator;
import com.redhat.console.integrations.EventAppender;
import com.redhat.console.integrations.EventPicker;

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
public class SplunkIntegration extends IntegrationsRouteBuilder {

    class SplunkHttpHeaderStrategy extends HttpHeaderFilterStrategy {
        @Override
        protected void initialize() {
            setLowerCase(true);
            setFilterOnMatch(false); // reverse filtering to only accept selected

            getInFilter().clear();
            getOutFilter().clear();
            getOutFilter().add("authorization");
        }
    }

    @Override
    public void configure() throws Exception {
        super.configure();
        configureHandler();
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
                // use default of 10 threads to process aggregated records (that act as queue)
                .parallelProcessing(true)
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
                .when(simple("${headers.metadata[trustAll]} == 'true'"))
                .to(https("dynamic")
                        .sslContextParameters(getTrustAllCACerts())
                        .x509HostnameVerifier(NoopHostnameVerifier.INSTANCE)
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

    protected SSLContextParameters getTrustAllCACerts() {
        TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
        trustManagersParameters.setTrustManager(new SplunkTrustAllCACerts());
        SSLContextParameters sslContextParameters = new SSLContextParameters();
        sslContextParameters.setTrustManagers(trustManagersParameters);

        return sslContextParameters;
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
