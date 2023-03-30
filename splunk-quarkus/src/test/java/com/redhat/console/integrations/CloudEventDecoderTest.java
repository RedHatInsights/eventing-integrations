package com.redhat.console.integrations;

import java.util.Map;

import com.redhat.console.integrations.testhelpers.CloudEventTestHelper;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.JsonObject;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.apache.camel.util.json.Jsoner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class CloudEventDecoderTest extends CamelQuarkusTestSupport {

    /**
     * Tests that the event decoder correctly processes the Cloud Event and that doesn't modify any of the incoming
     * data, even though it breaks it into a different structure for Camel.
     * 
     * @throws Exception if any unexpected error occurs.
     */
    @Test
    void testProcess() throws Exception {
        final JsonObject cloudEvent = CloudEventTestHelper.buildTestCloudEvent();

        final Exchange exchange = createExchangeWithBody(cloudEvent.toString());

        final CloudEventDecoder cloudEventDecoder = new CloudEventDecoder();

        // Call the decoder under test.
        cloudEventDecoder.process(exchange);

        // Get the message to test that it was correctly built.
        final Message message = exchange.getIn();

        // Check the headers. Everything except the "data" key should be set
        // as a header, plus the "metadata", "extras", "accountId" and the
        // "orgId" keys.
        final Map<String, Object> headers = message.getHeaders();
        Assertions.assertEquals(11, headers.size(), "unexpected number of headers generated in the decoder");

        Assertions.assertEquals(
                CloudEventTestHelper.TEST_ACTION_ID_VALUE.toString(),
                headers.get(String.format("Ce-%s", CloudEventTestHelper.FIELD_ID)),
                "the cloud event's ID field does not match");

        Assertions.assertEquals(
                CloudEventTestHelper.TEST_ACTION_ACCOUNT_ID,
                headers.get(String.format("Ce-%s", CloudEventTestHelper.FIELD_RH_ACCOUNT)),
                "the cloud event's 'rh-account' field does not match");

        Assertions.assertEquals(
                CloudEventTestHelper.TEST_ACTION_ORG_ID,
                headers.get(String.format("Ce-%s", CloudEventTestHelper.FIELD_RH_ORG_ID)),
                "the cloud event's 'rh-org-id' field does not match");

        Assertions.assertEquals(
                CloudEventTestHelper.FIELD_SOURCE_VALUE,
                headers.get(String.format("Ce-%s", CloudEventTestHelper.FIELD_SOURCE)),
                "the cloud event's 'source' field does not match");

        Assertions.assertEquals(
                CloudEventTestHelper.FIELD_SPEC_VERSION_VALUE,
                headers.get(String.format("Ce-%s", CloudEventTestHelper.FIELD_SPEC_VERSION)),
                "the cloud event's 'specversion' field does not match");

        Assertions.assertEquals(
                CloudEventTestHelper.TEST_ACTION_TIMESTAMP.toString(),
                headers.get(String.format("Ce-%s", CloudEventTestHelper.FIELD_TIME)),
                "the cloud event's 'time' field does not match");

        Assertions.assertEquals(
                CloudEventTestHelper.TEST_ACTION_ACCOUNT_ID,
                headers.get("accountId"),
                "the eventing-integrations' 'accountId' header does not match");

        Assertions.assertEquals(
                CloudEventTestHelper.TEST_ACTION_ORG_ID,
                headers.get("orgId"),
                "the eventing-integrations' 'orgId' header does not match");

        Assertions.assertEquals(
                CloudEventTestHelper.FIELD_NOTIF_METADATA_EXTRAS_VALUE,
                Jsoner.serialize(headers.get("extras")),
                "the eventing-integrations' 'extras' header does not match");

        // Get the metadata header.
        final org.apache.camel.util.json.JsonObject metadata = (org.apache.camel.util.json.JsonObject) headers.get("metadata");

        Assertions.assertEquals(
                CloudEventTestHelper.FIELD_NOTIF_METADATA_EXTRAS_VALUE,
                metadata.getString(CloudEventTestHelper.FIELD_NOTIF_METADATA_EXTRAS),
                "the 'extras' field on the 'notif-metadata' object has an unexpected value");

        Assertions.assertEquals(
                CloudEventTestHelper.TEST_ACTION_ID_VALUE.toString(),
                metadata.getString(CloudEventTestHelper.FIELD_NOTIF_METADATA_ORIGINAL_ID),
                "the 'id' field on the 'notif-metadata' object has an unexpected value");

        Assertions.assertEquals(
                CloudEventTestHelper.FIELD_NOTIF_METADATA_TRUST_ALL_VALUE,
                metadata.getString(CloudEventTestHelper.FIELD_NOTIF_METADATA_TRUST_ALL),
                "the 'trustAll' field on the 'notif-metadata' object has an unexpected value");

        Assertions.assertEquals(
                CloudEventTestHelper.FIELD_NOTIF_METADATA_TYPE_VALUE,
                metadata.getString(CloudEventTestHelper.FIELD_NOTIF_METADATA_TYPE),
                "the 'type' field on the 'notif-metadata' object has an unexpected value");

        Assertions.assertEquals(
                CloudEventTestHelper.FIELD_NOTIF_METADATA_URL_VALUE,
                metadata.getString(CloudEventTestHelper.FIELD_NOTIF_METADATA_URL),
                "the 'url' field on the 'notif-metadata' object has an unexpected value");

        Assertions.assertEquals(
                CloudEventTestHelper.FIELD_NOTIF_METADATA_X_INSIGHT_TOKEN_VALUE,
                metadata.getString(CloudEventTestHelper.FIELD_NOTIF_METADATA_X_INSIGHT_TOKEN),
                "the 'X-Insight-Token' field on the 'notif-metadata' object has an unexpected value");

        final org.apache.camel.util.json.JsonObject body = message.getBody(org.apache.camel.util.json.JsonObject.class);
        Assertions.assertEquals(CloudEventTestHelper.TEST_ACTION_VERSION, body.getString("version"),
                "the 'version' field of the action has an unexpected value");
        Assertions.assertEquals(CloudEventTestHelper.TEST_ACTION_ID_VALUE.toString(), body.getString("id"),
                "the 'id' field of the action has an unexpected value");
        Assertions.assertEquals(CloudEventTestHelper.TEST_ACTION_BUNDLE, body.getString("bundle"),
                "the 'bundle' field of the action has an unexpected value");
        Assertions.assertEquals(CloudEventTestHelper.TEST_ACTION_APPLICATION, body.getString("application"),
                "the 'application' field of the action has an unexpected value");
        Assertions.assertEquals(CloudEventTestHelper.TEST_ACTION_EVENT_TYPE, body.getString("event_type"),
                "the 'event_type' field of the action has an unexpected value");
        Assertions.assertEquals(CloudEventTestHelper.TEST_ACTION_TIMESTAMP.toString(), body.getString("timestamp"),
                "the 'timestamp' field of the action has an unexpected value");
        Assertions.assertEquals(CloudEventTestHelper.TEST_ACTION_ORG_ID, body.getString("org_id"),
                "the 'org_id' field of the action has an unexpected value");

        final org.apache.camel.util.json.JsonObject context = (org.apache.camel.util.json.JsonObject) body.get("context");
        Assertions.assertEquals(CloudEventTestHelper.TEST_ACTION_CONTEXT_INTEGRATION_UUID_VALUE.toString(),
                context.getString("integration-uuid"),
                "the 'integration-uuid' field of the action's context has an unexpected value");

        final org.apache.camel.util.json.JsonArray events = body.getCollection("events");
        Assertions.assertEquals(1, events.size(), "unexpected number of events found in the action");

        final org.apache.camel.util.json.JsonObject event = (org.apache.camel.util.json.JsonObject) events.get(0);
        final org.apache.camel.util.json.JsonObject eventMetadata
                = (org.apache.camel.util.json.JsonObject) event.get("metadata");
        Assertions.assertEquals(CloudEventTestHelper.TEST_ACTION_METADATA_VALUE,
                eventMetadata.getString(CloudEventTestHelper.TEST_ACTION_METADATA_KEY),
                "the 'metadata' field of the action's event has an unexpected value");

        final org.apache.camel.util.json.JsonObject eventPayload = (org.apache.camel.util.json.JsonObject) event.get("payload");
        Assertions.assertEquals(CloudEventTestHelper.TEST_ACTION_PAYLOAD_VALUE,
                eventPayload.getString(CloudEventTestHelper.TEST_ACTION_PAYLOAD_KEY),
                "the 'payload' field of the action's event has an unexpected value");
    }
}
