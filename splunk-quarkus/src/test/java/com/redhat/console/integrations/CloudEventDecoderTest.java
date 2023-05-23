package com.redhat.console.integrations;

import java.util.List;
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

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

@QuarkusTest
public class CloudEventDecoderTest extends CamelQuarkusTestSupport {

    /**
     * Tests that the event decoder correctly processes the Cloud Event and that doesn't modify any of the incoming
     * data, even though it breaks it into a different structure for Camel. It also tests that a cloud event which has
     * the "data" field as a string instead of a JSON object is parsed without any issues. For more information check
     * the comment in {@link CloudEventDecoder#process(Exchange)}.
     *
     * @throws Exception if any unexpected error occurs.
     */
    @Test
    void testProcess() throws Exception {
        final JsonObject cloudEvent = CloudEventTestHelper.buildTestCloudEvent();

        final JsonObject cloudEventDataAsString = CloudEventTestHelper.buildTestCloudEvent();
        // Extract the "data" key and store it again as a string.
        final JsonObject data = cloudEventDataAsString.getJsonObject(CloudEventTestHelper.FIELD_DATA);
        cloudEventDataAsString.put(CloudEventTestHelper.FIELD_DATA, data.toString());

        final List<JsonObject> cloudEvents = List.of(cloudEvent, cloudEventDataAsString);

        for (final JsonObject ce : cloudEvents) {
            final Exchange exchange = createExchangeWithBody(ce.encode());

            final CloudEventDecoder cloudEventDecoder = new CloudEventDecoder();

            // Call the decoder under test.
            cloudEventDecoder.process(exchange);

            this.assertExchangeIsCorrect(exchange);
        }
    }

    /**
     * Asserts that the provided exchange contains all the information that came in the incoming message, and that the
     * transformations it performs are correct.
     *
     * @param exchange the exchange to assert.
     */
    void assertExchangeIsCorrect(final Exchange exchange) {
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
                CloudEventTestHelper.TEST_ACTION_TIMESTAMP.format(ISO_DATE_TIME),
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

        CloudEventTestHelper.assertEventIsTheExpectedOne(new JsonObject(body.toJson()));
    }
}
