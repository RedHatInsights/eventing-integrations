package com.redhat.console.integrations.splunk;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.redhat.console.integrations.CloudEventDecoder;
import com.redhat.console.integrations.testhelpers.CloudEventTestHelper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.json.JsonObject;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(SplunkIntegrationInvalidUrlTest.class)
public class SplunkIntegrationInvalidUrlTest extends CamelQuarkusTestSupport {
    /**
     * Signal Camel that when we manipulate the endpoints the routes shouldn't be restarted.
     * <a href="https://camel.apache.org/manual/advice-with.html#_enabling_advice_during_testing"> More information
     * here.</a>
     *
     * @return true.
     */
    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    /**
     * Specifically creates the Splunk integration's routes.
     *
     * @return the Splunk integration's route builder.
     */
    @Override
    protected RouteBuilder createRouteBuilder() {
        return new SplunkIntegration();
    }

    /**
     * Tests that when a "target URL" is specified with an invalid URL, then the "URL Validation failed" error is
     * received in the Cloud Event's payload.
     *
     * @throws Exception if any unexpected error occurs.
     */
    @Test
    void testInvalidUrl() throws Exception {
        // Mock all the "https" endpoints to avoid failures.
        AdviceWith.adviceWith(this.context, "handler", a -> a.mockEndpoints("https:*"));

        // Make the ".to" output of the "secureConnectionFailed" route to a
        // mocked endpoint,
        AdviceWith.adviceWith(
                this.context,
                "targetUrlValidationFailed",
                a -> a.weaveByToUri("direct://return").replace().to("mock:return"));

        // Mock all the endpoints from the "return" route to avoid accidentally
        // hitting Kafka.
        AdviceWith.adviceWith(this.context, "return", AdviceWithRouteBuilder::mockEndpoints);

        // Create the payload.
        final JsonObject cloudEvent = CloudEventTestHelper.buildTestCloudEvent();
        final JsonObject data = cloudEvent.getJsonObject(CloudEventTestHelper.FIELD_DATA);

        // Update the "url" field and set an invalid URL, which should be
        // rejected by the "TargetUrlValidator".
        JsonObject notifMetadata = data.getJsonObject(CloudEventTestHelper.FIELD_NOTIF_METADATA);
        notifMetadata.put(CloudEventTestHelper.FIELD_NOTIF_METADATA_URL, "https://Hello, World!");

        final Exchange invalidUrlExchange = createExchangeWithBody(cloudEvent.toString());

        // Call the decoder to mimic what the exchange will look like when the
        // SplunkIntegration route receives the payload.
        final CloudEventDecoder cloudEventDecoder = new CloudEventDecoder();
        cloudEventDecoder.process(invalidUrlExchange);

        // Make sure that the mocked output receives the expected exchange.
        final MockEndpoint mockedReturnEndpoint = getMockEndpoint("mock:return");
        mockedReturnEndpoint.expectedMessageCount(1);

        this.template.send("direct:handler", invalidUrlExchange);

        MockEndpoint.assertIsSatisfied(1000, TimeUnit.MILLISECONDS, mockedReturnEndpoint);

        // Get the incoming exchange and message. We should only have received
        // one.
        final List<Exchange> receivedExchanges = mockedReturnEndpoint.getExchanges();
        Assertions.assertEquals(1, receivedExchanges.size(), "only one exchange should have been received");
        final Exchange receivedExchange = receivedExchanges.get(0);
        final Message receivedMessage = receivedExchange.getIn();

        CloudEventTestHelper.assertOutcomeAndSuccessfulAre(receivedMessage.getBody(String.class), "URL Validation failed",
                false);
    }
}
