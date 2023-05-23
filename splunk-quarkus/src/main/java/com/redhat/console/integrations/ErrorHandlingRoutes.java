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
package com.redhat.console.integrations;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.camel.LoggingLevel;

import static com.redhat.console.integrations.OutgoingCloudEventBuilder.OUTCOME_EXCHANGE_PROPERTY;
import static com.redhat.console.integrations.OutgoingCloudEventBuilder.SUCCESSFUL_EXCHANGE_PROPERTY;

/**
 * Configures Error Routes
 */

@ApplicationScoped
public class ErrorHandlingRoutes extends IntegrationsRouteBuilder {

    @Inject
    OutgoingCloudEventBuilder outgoingCloudEventBuilder;

    @Override
    public void configure() throws Exception {
        super.configure();

        configureIoFailed();
        configureHttpFailed();
        configureTargetUrlValidationFailed();
        configureSecureConnectionFailed();
    }

    private void configureSecureConnectionFailed() {
        // The error handler when we receive an HTTP (unsecure) connection instead of HTTPS
        from(direct("secureConnectionFailed"))
                .routeId("secureConnectionFailed")
                .log(LoggingLevel.ERROR, "ProtocolException for event ${header.ce-id} (orgId ${header.orgId}"
                                         + " account ${header.accountId}) to ${exchangeProperty.targetUrl}: ${exception.message}")
                .log(LoggingLevel.DEBUG, "${exception.stacktrace}")
                .setProperty(OUTCOME_EXCHANGE_PROPERTY, simple("${exception.message}"))
                .setProperty(SUCCESSFUL_EXCHANGE_PROPERTY, constant(false))
                .process(outgoingCloudEventBuilder)
                .to(direct("return"));
    }

    private void configureTargetUrlValidationFailed() {
        // The error handler when we receive a TargetUrlValidator failure
        from(direct("targetUrlValidationFailed"))
                .routeId("targetUrlValidationFailed")
                .log(LoggingLevel.ERROR, "IllegalArgumentException for event ${header.ce-id} (orgId ${header.orgId}"
                                         + " account ${header.accountId}) to ${exchangeProperty.targetUrl}: ${exception.message}")
                .log(LoggingLevel.DEBUG, "${exception.stacktrace}")
                .setProperty(OUTCOME_EXCHANGE_PROPERTY, simple("${exception.message}"))
                .setProperty(SUCCESSFUL_EXCHANGE_PROPERTY, constant(false))
                .process(outgoingCloudEventBuilder)
                .to(direct("return"));
    }

    private void configureIoFailed() {
        // The error handler found an IO Exception. We set the outcome to fail and then send to kafka
        from(direct("ioFailed"))
                .routeId("ioFailed")
                .log(LoggingLevel.ERROR, "IOFailure for event ${header.ce-id} (orgId ${header.orgId}"
                                         + " account ${header.accountId}) to ${exchangeProperty.targetUrl}: ${exception.message}")
                .log(LoggingLevel.DEBUG, "${exception.stacktrace}")
                .setProperty(OUTCOME_EXCHANGE_PROPERTY, simple("${exception.message}"))
                .setProperty(SUCCESSFUL_EXCHANGE_PROPERTY, constant(false))
                .process(outgoingCloudEventBuilder)
                .to(direct("return"));
    }

    private void configureHttpFailed() {
        // The error handler found an HTTP Exception. We set the outcome to fail and then send to kafka
        from(direct("httpFailed"))
                .routeId("httpFailed")
                .log(LoggingLevel.ERROR, "HTTPFailure for event ${header.ce-id} (orgId ${header.orgId} account"
                                         + " ${header.accountId}) to ${exchangeProperty.targetUrl}: ${exception.getStatusCode()}"
                                         + " ${exception.getStatusText()}: ${exception.message}")
                .log(LoggingLevel.DEBUG, "Response Body: ${exception.getResponseBody()}")
                .log(LoggingLevel.DEBUG, "Response Headers: ${exception.getResponseHeaders()}")
                .setProperty(OUTCOME_EXCHANGE_PROPERTY, simple("${exception.message}"))
                .setProperty(SUCCESSFUL_EXCHANGE_PROPERTY, constant(false))
                .process(outgoingCloudEventBuilder)
                .to(direct("return"));
    }
}
