package com.redhat.console.integrations;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.util.json.JsonObject;
import org.apache.camel.util.json.Jsoner;

/**
 * We decode a CloudEvent, set the headers accordingly and put the CE payload as the new body
 */
public class CloudEventDecoder implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {

        Message in = exchange.getIn();
        String body = in.getBody(String.class);
        JsonObject ceIn = (JsonObject) Jsoner.deserialize(body);
        for (String key : ceIn.keySet()) {
            if (key.equals("data")) {
                continue; // This is the data. We don't put it in the header
            }
            in.setHeader("Ce-" + key, ceIn.getString(key));
        }

        // Extract the "data" field from the incoming payload. Probably due to
        // an unintended mistake, the Notifications Engine sends the "data"'s
        // JSON as a quoted string, instead as a JSON object itself. Our
        // console.redhat.com CloudEvents specification, which in turn adheres
        // to the Cloud Events specification, defines the "data" key as a JSON
        // object[1], and therefore here we attempt to parse it as it is
        // defined in the specification. However, a fallback of the original
        // parsing way is left too, in order to avoid breaking the integration
        // until the bug is fixed in the Notifications Engine. More information
        // about this can be found in the RHCLOUD-24986[2] Jira ticket.
        //
        // [1]: https://github.com/RedHatInsights/event-schemas/blob/4119fc6e3820c6519f8f87abc27cdb4604a80db0/schemas/events/v1/events.json#L58-L59
        // [2]: https://issues.redhat.com/browse/RHCLOUD-24986
        JsonObject bodyObject;
        try {
            bodyObject = (JsonObject) ceIn.get("data");
        } catch (final ClassCastException e) {
            bodyObject = (JsonObject) Jsoner.deserialize(ceIn.getString("data"));
        }

        // Extract metadata, put it in headers and then delete from the body.
        JsonObject metaData = (JsonObject) bodyObject.get("notif-metadata");
        in.setHeader("metadata", metaData);
        JsonObject extras = (JsonObject) Jsoner.deserialize(metaData.getString("extras"));
        in.setHeader("extras", extras);
        in.setHeader("accountId", bodyObject.get("account_id"));
        in.setHeader("orgId", bodyObject.get("org_id"));

        bodyObject.remove("notif-metadata");

        in.setBody(bodyObject);
    }
}
