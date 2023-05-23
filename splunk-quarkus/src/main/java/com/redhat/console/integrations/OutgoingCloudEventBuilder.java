package com.redhat.console.integrations;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.util.json.JsonObject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Transformer to create a Map from the outcome of the actual component.
 */
/**
 * Encode the passed body in a CloudEvent, marshalled as Json
 */
@ApplicationScoped
public class OutgoingCloudEventBuilder implements Processor {

    public static final String OUTCOME_EXCHANGE_PROPERTY = "outcome";
    public static final String SUCCESSFUL_EXCHANGE_PROPERTY = "successful";

    private static final String CE_SPEC_VERSION = "1.0";
    private static final String CE_TYPE = "com.redhat.console.notifications.history";

    @ConfigProperty(name = "integrations.component.name")
    String source;

    public void process(Exchange exchange) throws Exception {

        Message in = exchange.getIn();

        JsonObject details = new JsonObject();
        details.put("target", exchange.getProperty("targetUrl", String.class));
        details.put("type", in.getHeader("Ce-type", String.class));
        details.put("outcome", exchange.getProperty(OUTCOME_EXCHANGE_PROPERTY, String.class));

        JsonObject data = new JsonObject();
        data.put("successful", exchange.getProperty(SUCCESSFUL_EXCHANGE_PROPERTY, Boolean.class));
        // TODO use header("kafka.TIMESTAMP") ?
        data.put("duration", System.currentTimeMillis() - exchange.getProperty("timeIn", Long.class));
        data.put("details", details);

        JsonObject outgoingCloudEvent = new JsonObject();
        outgoingCloudEvent.put("type", CE_TYPE);
        outgoingCloudEvent.put("specversion", CE_SPEC_VERSION);
        outgoingCloudEvent.put("source", source);
        outgoingCloudEvent.put("id", in.getHeader("ce-id", String.class));
        outgoingCloudEvent.put("time", LocalDateTime.now(ZoneOffset.UTC).toString());
        // TODO The serialization to JSON shouldn't be needed here. Migrate this later!
        outgoingCloudEvent.put("data", data.toJson());

        /*
         * Removes all headers that start with "ce-".
         * The header name is converted to lower case before the prefix match is tested.
         */
        in.removeHeaders("ce-*");
        in.setBody(outgoingCloudEvent.toJson());
    }
}
