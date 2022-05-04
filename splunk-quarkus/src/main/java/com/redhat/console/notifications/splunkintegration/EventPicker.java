package com.redhat.console.notifications.splunkintegration;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.util.json.JsonObject;
import org.apache.camel.util.json.JsonArray;
import org.apache.camel.util.json.Jsoner;

/**
 * Transformer to pick an Event from the events of the message.
 */
public class EventPicker implements Processor {

    public void process(Exchange exchange) throws Exception {

        Message in = exchange.getIn();
        String oldBody = in.getBody(String.class);
        JsonObject jsonBody = (JsonObject) Jsoner.deserialize(oldBody);

        JsonArray events = jsonBody.getCollection("events");
        JsonArray newEvents = new JsonArray();

        // asks Exchange for an index which is populated by a loop
        Integer index = (Integer) exchange.getProperty("CamelLoopIndex");

        newEvents.add(events.get(index));

        jsonBody.put("events", newEvents);
        String bodyAsJsonString = jsonBody.toJson();

        in.setBody(bodyAsJsonString);
    }

}
