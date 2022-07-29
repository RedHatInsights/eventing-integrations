package com.redhat.console.notifications.splunkintegration;

import java.io.IOException;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.http.ProtocolException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

abstract class ErrorHandler extends EndpointRouteBuilder {

  

  @ConfigProperty(name = "integrations.component.name")
  String COMPONENT_NAME;

  public String LOGGER_NAME;
  
  public static final String RETURN_TYPE = "com.redhat.console.notifications.history";

  @Override
  public void configure() throws Exception {
    LOGGER_NAME = "com.redhat.console.notification.toCamel." + COMPONENT_NAME;

    getContext().getGlobalOptions().put(Exchange.LOG_EIP_NAME, LOGGER_NAME);
    configureErrorHandler(); // reusable
    configureIoFailed(); // reusable
    configureHttpFailed(); // reusable
    configureTargetUrlValidationFailed(); // reusable
    configureSecureConnectionFailed(); // reusable
  }

  private void configureErrorHandler() throws Exception {
    onException(IOException.class)
        .to(direct("ioFailed"))
        .handled(true);
    onException(HttpOperationFailedException.class)
        .to(direct("httpFailed"))
        .handled(true);
    onException(IllegalArgumentException.class)
        .to(direct("targetUrlValidationFailed"))
        .handled(true);
    onException(ProtocolException.class)
        .to(direct("secureConnectionFailed"))
        .handled(true);
  }

  private void configureSecureConnectionFailed() throws Exception {
    Processor ceEncoder = new CloudEventEncoder(COMPONENT_NAME, RETURN_TYPE);
    Processor resultTransformer = new ResultTransformer();
    // The error handler when we receive an HTTP (unsecure) connection instead of
    // HTTPS
    from(direct("secureConnectionFailed"))
        .routeId("secureConnectionFailed")
        .log(LoggingLevel.ERROR, "ProtocolException for event ${header.ce-id} (orgId ${header.orgId}"
                                         + " account ${header.accountId}) to ${header.targetUrl}: ${exception.message}")
        .log(LoggingLevel.DEBUG, "${exception.stacktrace}")
        .setBody(simple("${exception.message}"))
        .setHeader("outcome-fail", simple("true"))
        .process(resultTransformer)
        .marshal().json()
        .process(ceEncoder)
        .to(direct("return"));
  }

  private void configureTargetUrlValidationFailed() throws Exception {
    Processor ceEncoder = new CloudEventEncoder(COMPONENT_NAME, RETURN_TYPE);
    Processor resultTransformer = new ResultTransformer();
    // The error handler when we receive a TargetUrlValidator failure
    from(direct("targetUrlValidationFailed"))
        .routeId("targetUrlValidationFailed")
        .log(LoggingLevel.ERROR, "IllegalArgumentException for event ${header.ce-id} (orgId ${header.orgId}"
        + " account ${header.accountId}) to ${header.targetUrl}: ${exception.message}")
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
    // The error handler found an IO Exception. We set the outcome to fail and then
    // send to kafka
    from(direct("ioFailed"))
        .routeId("ioFailed")
        .log(LoggingLevel.ERROR, "IOFailure for event ${header.ce-id} (orgId ${header.orgId}"
        + " account ${header.accountId}) to ${header.targetUrl}: ${exception.message}")
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
    // The error handler found an HTTP Exception. We set the outcome to fail and
    // then send to kafka
    from(direct("httpFailed"))
        .routeId("httpFailed")
        .log(LoggingLevel.ERROR, "HTTPFailure for event ${header.ce-id} (orgId ${header.orgId} account"
        + " ${header.accountId}) to ${header.targetUrl}: ${exception.getStatusCode()}"
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
}
