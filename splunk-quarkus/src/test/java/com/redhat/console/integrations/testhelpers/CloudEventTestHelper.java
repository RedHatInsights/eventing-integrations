package com.redhat.console.integrations.testhelpers;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.redhat.cloud.notifications.ingress.Action;
import com.redhat.cloud.notifications.ingress.Context;
import com.redhat.cloud.notifications.ingress.Event;
import com.redhat.cloud.notifications.ingress.Metadata;
import com.redhat.cloud.notifications.ingress.Payload;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CloudEventTestHelper {
    /**
     * Cloud event's top level fields.
     */
    public static final String FIELD_DATA = "data";
    public static final String FIELD_ID = "id";
    public static final String FIELD_RH_ACCOUNT = "rh-account";
    public static final String FIELD_RH_ORG_ID = "rh-org-id";
    public static final String FIELD_SOURCE = "source";
    public static final String FIELD_SOURCE_VALUE = "notifications";
    public static final String FIELD_SPEC_VERSION = "specversion";
    public static final String FIELD_SPEC_VERSION_VALUE = "1.0.0";
    public static final String FIELD_TIME = "time";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_TYPE_VALUE = "com.redhat.console.notification.toCamel.splunk";
    /**
     * Notif-metadata fields.
     */
    public static final String FIELD_NOTIF_METADATA = "notif-metadata";
    public static final String FIELD_NOTIF_METADATA_EXTRAS = "extras";
    public static final String FIELD_NOTIF_METADATA_EXTRAS_VALUE = "{\"hello\":\"world\"}";
    public static final String FIELD_NOTIF_METADATA_ORIGINAL_ID = "_originalId";
    public static final String FIELD_NOTIF_METADATA_TRUST_ALL = "trustAll";
    public static final String FIELD_NOTIF_METADATA_TRUST_ALL_VALUE = Boolean.toString(false);
    public static final String FIELD_NOTIF_METADATA_TYPE = "type";
    public static final String FIELD_NOTIF_METADATA_TYPE_VALUE = "splunk";
    public static final String FIELD_NOTIF_METADATA_URL = "url";
    public static final String FIELD_NOTIF_METADATA_URL_VALUE = "https://example.com";
    public static final String FIELD_NOTIF_METADATA_X_INSIGHT_TOKEN = "X-Insight-Token";
    public static final String FIELD_NOTIF_METADATA_X_INSIGHT_TOKEN_VALUE = "splunk secret token";
    /**
     * Regular action test data.
     */
    public static final LocalDateTime TEST_ACTION_TIMESTAMP = LocalDateTime.now(Clock.systemUTC());
    public static final String TEST_ACTION_ACCOUNT_ID = "account-id";
    public static final String TEST_ACTION_APPLICATION = "integrations";
    public static final String TEST_ACTION_BUNDLE = "console";
    public static final String TEST_ACTION_CONTEXT_INTEGRATION_UUID = "integration-uuid";
    public static final String TEST_ACTION_EVENT_TYPE = "integration-test";
    public static final String TEST_ACTION_METADATA_KEY = "metadata-key";
    public static final String TEST_ACTION_METADATA_VALUE = "metadata-value";
    public static final String TEST_ACTION_ORG_ID = "test-org-id";
    public static final String TEST_ACTION_PAYLOAD_KEY = "message";
    public static final String TEST_ACTION_PAYLOAD_VALUE
            = "Congratulations! The integration you created on https://console.redhat.com was successfully tested!";
    public static final String TEST_ACTION_VERSION = "2.0.0";
    public static final UUID TEST_ACTION_CONTEXT_INTEGRATION_UUID_VALUE = UUID.randomUUID();
    public static final UUID TEST_ACTION_ID_VALUE = UUID.randomUUID();

    /**
     * Builds a test cloud event.
     *
     * @return           the generated cloud event.
     * @throws Exception if any unexpected error occurs.
     */
    public static JsonObject buildTestCloudEvent() throws Exception {
        // Create a test action.
        final Action action = createTestAction();

        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.registerModule(new JavaTimeModule());

        final JsonObject data = new JsonObject(objectMapper.writeValueAsString(action));

        // Add the "notif-metadata" object to the action.
        final JsonObject notifMetadata = new JsonObject();
        notifMetadata.put(FIELD_NOTIF_METADATA_EXTRAS, FIELD_NOTIF_METADATA_EXTRAS_VALUE);
        notifMetadata.put(FIELD_NOTIF_METADATA_ORIGINAL_ID, TEST_ACTION_ID_VALUE);
        notifMetadata.put(FIELD_NOTIF_METADATA_TRUST_ALL, FIELD_NOTIF_METADATA_TRUST_ALL_VALUE);
        notifMetadata.put(FIELD_NOTIF_METADATA_TYPE, FIELD_NOTIF_METADATA_TYPE_VALUE);
        notifMetadata.put(FIELD_NOTIF_METADATA_URL, FIELD_NOTIF_METADATA_URL_VALUE);
        notifMetadata.put(FIELD_NOTIF_METADATA_X_INSIGHT_TOKEN, FIELD_NOTIF_METADATA_X_INSIGHT_TOKEN_VALUE);

        data.put(FIELD_NOTIF_METADATA, notifMetadata);

        // Create a test cloud event.
        final JsonObject cloudEvent = new JsonObject();
        cloudEvent.put(FIELD_SPEC_VERSION, FIELD_SPEC_VERSION_VALUE);
        cloudEvent.put(FIELD_TYPE, FIELD_TYPE_VALUE);
        cloudEvent.put(FIELD_SOURCE, FIELD_SOURCE_VALUE);
        cloudEvent.put(FIELD_ID, TEST_ACTION_ID_VALUE);
        cloudEvent.put(FIELD_TIME, TEST_ACTION_TIMESTAMP);
        cloudEvent.put(FIELD_RH_ORG_ID, TEST_ACTION_ORG_ID);
        cloudEvent.put(FIELD_RH_ACCOUNT, TEST_ACTION_ACCOUNT_ID);
        cloudEvent.put(FIELD_DATA, data);

        return cloudEvent;
    }

    /**
     * Creates a test action that is what the "Notifications" service would send to the eventing backend.
     *
     * @return the created action.
     */
    public static Action createTestAction() {
        final Action testAction = new Action();

        final Context context = new Context();
        context.setAdditionalProperty(TEST_ACTION_CONTEXT_INTEGRATION_UUID, TEST_ACTION_CONTEXT_INTEGRATION_UUID_VALUE);

        testAction.setContext(context);

        /*
         * Create a test event which will be sent along with the action.
         */
        final Event testEvent = new Event();

        final Metadata metadata = new Metadata();
        metadata.setAdditionalProperty(TEST_ACTION_METADATA_KEY, TEST_ACTION_METADATA_VALUE);

        final Payload payload = new Payload();
        payload.setAdditionalProperty(TEST_ACTION_PAYLOAD_KEY, TEST_ACTION_PAYLOAD_VALUE);

        testEvent.setMetadata(metadata);
        testEvent.setPayload(payload);

        /*
         * Set the rest of the action's members.
         */
        testAction.setAccountId(TEST_ACTION_ACCOUNT_ID);
        testAction.setApplication(TEST_ACTION_APPLICATION);
        testAction.setBundle(TEST_ACTION_BUNDLE);
        testAction.setEvents(List.of(testEvent));
        testAction.setEventType(TEST_ACTION_EVENT_TYPE);
        testAction.setId(TEST_ACTION_ID_VALUE);
        testAction.setOrgId(TEST_ACTION_ORG_ID);
        testAction.setTimestamp(TEST_ACTION_TIMESTAMP);
        testAction.setVersion(TEST_ACTION_VERSION);

        return testAction;
    }

    /**
     * Asserts that the received event has the fields and the values that were generated with this class'
     * {@link #buildTestCloudEvent()} function.
     *
     * @param event the event to evaluate.
     */
    public static void assertEventIsTheExpectedOne(final JsonObject event) {
        assertEquals(CloudEventTestHelper.TEST_ACTION_VERSION, event.getString("version"),
                "the 'version' field of the action has an unexpected value");
        assertEquals(CloudEventTestHelper.TEST_ACTION_ID_VALUE.toString(), event.getString("id"),
                "the 'id' field of the action has an unexpected value");
        assertEquals(CloudEventTestHelper.TEST_ACTION_BUNDLE, event.getString("bundle"),
                "the 'bundle' field of the action has an unexpected value");
        assertEquals(CloudEventTestHelper.TEST_ACTION_APPLICATION, event.getString("application"),
                "the 'application' field of the action has an unexpected value");
        assertEquals(CloudEventTestHelper.TEST_ACTION_EVENT_TYPE, event.getString("event_type"),
                "the 'event_type' field of the action has an unexpected value");
        assertEquals(CloudEventTestHelper.TEST_ACTION_TIMESTAMP.format(ISO_DATE_TIME), event.getString("timestamp"),
                "the 'timestamp' field of the action has an unexpected value");
        assertEquals(CloudEventTestHelper.TEST_ACTION_ORG_ID, event.getString("org_id"),
                "the 'org_id' field of the action has an unexpected value");

        final JsonObject context = event.getJsonObject("context");
        assertEquals(CloudEventTestHelper.TEST_ACTION_CONTEXT_INTEGRATION_UUID_VALUE.toString(),
                context.getString("integration-uuid"),
                "the 'integration-uuid' field of the action's context has an unexpected value");

        final JsonArray events = event.getJsonArray("events");
        assertEquals(1, events.size(), "unexpected number of events found in the action");

        final JsonObject innerEvent = events.getJsonObject(0);
        final JsonObject eventMetadata = innerEvent.getJsonObject("metadata");
        assertEquals(CloudEventTestHelper.TEST_ACTION_METADATA_VALUE,
                eventMetadata.getString(CloudEventTestHelper.TEST_ACTION_METADATA_KEY),
                "the 'metadata' field of the action's event has an unexpected value");

        final JsonObject eventPayload = innerEvent.getJsonObject("payload");
        assertEquals(CloudEventTestHelper.TEST_ACTION_PAYLOAD_VALUE,
                eventPayload.getString(CloudEventTestHelper.TEST_ACTION_PAYLOAD_KEY),
                "the 'payload' field of the action's event has an unexpected value");
    }

    /**
     * Asserts that the given Splunk payload is generated from the data that was generated in
     * {@link #buildTestCloudEvent()}. The "events" array is not validated since that part is different for every Splunk
     * payload.
     *
     * @param event the event to evaluate.
     */
    public static void assertSplunkPayloadIsTheExpectedOne(final JsonObject event) {
        // Assert the hard coded fields from the payload.
        assertEquals("eventing", event.getString("source"));
        assertEquals("Insights event", event.getString("sourcetype"));

        final JsonObject innerEvent = event.getJsonObject("event");

        assertEquals(CloudEventTestHelper.TEST_ACTION_VERSION, innerEvent.getString("version"),
                "the 'version' field of the action has an unexpected value");
        assertEquals(CloudEventTestHelper.TEST_ACTION_ID_VALUE.toString(), innerEvent.getString("id"),
                "the 'id' field of the action has an unexpected value");
        assertEquals(CloudEventTestHelper.TEST_ACTION_BUNDLE, innerEvent.getString("bundle"),
                "the 'bundle' field of the action has an unexpected value");
        assertEquals(CloudEventTestHelper.TEST_ACTION_APPLICATION, innerEvent.getString("application"),
                "the 'application' field of the action has an unexpected value");
        assertEquals(CloudEventTestHelper.TEST_ACTION_EVENT_TYPE, innerEvent.getString("event_type"),
                "the 'event_type' field of the action has an unexpected value");
        assertEquals(CloudEventTestHelper.TEST_ACTION_TIMESTAMP.format(ISO_DATE_TIME),
                innerEvent.getString("timestamp"),
                "the 'timestamp' field of the action has an unexpected value");
        assertEquals(CloudEventTestHelper.TEST_ACTION_ORG_ID, innerEvent.getString("org_id"),
                "the 'org_id' field of the action has an unexpected value");

        final JsonObject context = innerEvent.getJsonObject("context");
        assertEquals(CloudEventTestHelper.TEST_ACTION_CONTEXT_INTEGRATION_UUID_VALUE.toString(),
                context.getString("integration-uuid"),
                "the 'integration-uuid' field of the action's context has an unexpected value");
    }

    /**
     * Asserts that the received body contains the specified expected message in the "outcome" field.
     *
     * @param receivedBody    the received body from Apache Camel.
     * @param expectedMessage the expected message to find in the "outcome" field of the body.
     */
    public static void assertOutcomeAndSuccessfulAre(
            final String receivedBody, final String expectedMessage, boolean expectedSuccessful) {
        final JsonObject receivedJson = new JsonObject(receivedBody);
        assertNotNull(receivedBody, "the body of the message is empty");

        final JsonObject receivedData = new JsonObject(receivedJson.getString(CloudEventTestHelper.FIELD_DATA));
        assertNotNull(receivedData, "the cloud event's \"data\" field is missing");

        JsonObject receivedDetails = receivedData.getJsonObject("details");
        assertNotNull(receivedData, "the cloud event's \"data.details\" field is missing");

        final String outcome = receivedDetails.getString("outcome");
        assertEquals(expectedMessage, outcome, "unexpected error message received");

        assertEquals(expectedSuccessful, receivedData.getBoolean("successful"), "unexpected successful field value");
    }
}
