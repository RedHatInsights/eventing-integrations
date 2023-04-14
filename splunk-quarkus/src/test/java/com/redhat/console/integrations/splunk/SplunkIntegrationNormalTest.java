package com.redhat.console.integrations.splunk;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.console.integrations.CloudEventDecoder;
import com.redhat.console.integrations.testhelpers.CloudEventTestHelper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(SplunkIntegrationNormalTest.class)
public class SplunkIntegrationNormalTest extends CamelQuarkusTestSupport {
    public boolean isUseAdviceWith() {
        return true;
    }

    public RouteBuilder createRouteBuilder() {
        return new SplunkIntegration();
    }

    /**
     * Tests that under normal circumstances, the Splunk Integration route separates the events in the "events" array of
     * the Cloud Event, into separate payloads which then Apache Camel sends individually.
     * 
     * @throws Exception if any unexpected error occurs.
     */
    @Test
    void testNormal() throws Exception {
        AdviceWith.adviceWith(this.context, "handler", a -> a.mockEndpoints("https:*"));
        AdviceWith.adviceWith(this.context, "return", AdviceWithRouteBuilder::mockEndpoints);
        AdviceWith.adviceWith(this.context, "success", AdviceWithRouteBuilder::mockEndpoints);

        // Create the payload.
        final JsonObject cloudEvent = CloudEventTestHelper.buildTestCloudEvent();
        final JsonObject data = cloudEvent.getJsonObject(CloudEventTestHelper.FIELD_DATA);

        // Update the events to add another one to the array.
        final JsonArray events = data.getJsonArray("events");

        final JsonObject metadata = new JsonObject();
        final String metadataTwoKey = "metadata-2-key";
        final String metadataTwoValue = "metadata-2-value";
        metadata.put(metadataTwoKey, metadataTwoValue);

        final JsonObject payload = new JsonObject();
        final String payloadKey = "custom-field";
        final String payloadValue = "custom-value";
        payload.put(payloadKey, payloadValue);

        final JsonObject event = new JsonObject();
        event.put("metadata", metadata);
        event.put("payload", payload);

        events.add(event);

        final Exchange validExchange = createExchangeWithBody(cloudEvent.toString());

        // Call the decoder to mimic what the exchange will look like when the
        // SplunkIntegration route receives the payload.
        final CloudEventDecoder cloudEventDecoder = new CloudEventDecoder();
        cloudEventDecoder.process(validExchange);

        // Make sure that the mocked output receives the expected exchange.
        final MockEndpoint mockedDynamicEndpoint = getMockEndpoint("mock:https:dynamic");
        mockedDynamicEndpoint.expectedMessageCount(1);

        this.template.send("direct:handler", validExchange);

        MockEndpoint.assertIsSatisfied(2000, TimeUnit.MILLISECONDS, mockedDynamicEndpoint);

        final List<Exchange> exchanges = mockedDynamicEndpoint.getExchanges();
        final Exchange exchange = exchanges.get(0);
        final String body = exchange.getIn().getBody(String.class);

        // The Splunk Integration route will aggregate the bodies together, so
        // we need to split them back.
        final Pattern pattern = Pattern.compile("^(?<one>\\{.+})(?<two>\\{.+})$");
        final Matcher matcher = pattern.matcher(body);

        Assertions.assertTrue(matcher.find(),
                "the logic appends the two events in two separate payloads, but the matcher was not able to find them");

        final JsonObject firstPayload = new JsonObject(matcher.group("one"));
        final JsonObject secondPayload = new JsonObject(matcher.group("two"));

        // Assert that the common bits of the payloads are the same.
        CloudEventTestHelper.assertSplunkPayloadIsTheExpectedOne(firstPayload);
        CloudEventTestHelper.assertSplunkPayloadIsTheExpectedOne(secondPayload);

        // Assert that the different events on the payloads are the expected
        // ones.
        final JsonObject firstPayloadEvent = firstPayload.getJsonObject("event");
        final JsonArray firstPayloadEvents = firstPayloadEvent.getJsonArray("events");

        Assertions.assertEquals(1, firstPayloadEvents.size(), "unexpected number of events found in the action");

        final JsonObject firstPayloadInnerEvent = firstPayloadEvents.getJsonObject(0);

        final JsonObject firstPayloadEventMetadata = firstPayloadInnerEvent.getJsonObject("metadata");
        Assertions.assertEquals(CloudEventTestHelper.TEST_ACTION_METADATA_VALUE,
                firstPayloadEventMetadata.getString(CloudEventTestHelper.TEST_ACTION_METADATA_KEY),
                "the 'metadata' field of the action's event has an unexpected value");

        final JsonObject firstEventPayload = firstPayloadInnerEvent.getJsonObject("payload");
        Assertions.assertEquals(CloudEventTestHelper.TEST_ACTION_PAYLOAD_VALUE,
                firstEventPayload.getString(CloudEventTestHelper.TEST_ACTION_PAYLOAD_KEY),
                "the 'payload' field of the action's event has an unexpected value");

        // Assert that the second payload's event contains the expected data.
        final JsonObject secondPayloadEvent = secondPayload.getJsonObject("event");
        final JsonArray secondPayloadEvents = secondPayloadEvent.getJsonArray("events");

        Assertions.assertEquals(1, secondPayloadEvents.size(), "unexpected number of events found in the action");

        final JsonObject secondPayloadInnerEvent = secondPayloadEvents.getJsonObject(0);

        final JsonObject secondPayloadEventMetadata = secondPayloadInnerEvent.getJsonObject("metadata");
        Assertions.assertEquals(
                metadataTwoValue,
                secondPayloadEventMetadata.getString(metadataTwoKey),
                "the 'metadata' field of the action's event has an unexpected value");

        final JsonObject secondEventPayload = secondPayloadInnerEvent.getJsonObject("payload");
        Assertions.assertEquals(
                payloadValue,
                secondEventPayload.getString(payloadKey),
                "the 'payload' field of the action's event has an unexpected value");
    }
}
