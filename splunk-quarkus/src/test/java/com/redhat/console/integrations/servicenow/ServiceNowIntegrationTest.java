package com.redhat.console.integrations.servicenow;

import java.nio.charset.Charset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
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
@TestProfile(ServiceNowOverrides.class)
public class ServiceNowIntegrationTest extends CamelQuarkusTestSupport {
    @Override
    public RouteBuilder createRouteBuilder() {
        return new ServiceNowIntegration();
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    /**
     * Tests that a proper basic authentication header and a proper payload are sent to Service Now.
     * 
     * @throws Exception if any unexpected error occurs.
     */
    @Test
    void testNormal() throws Exception {
        AdviceWith.adviceWith(this.context, "handler", AdviceWithRouteBuilder::mockEndpoints);
        AdviceWith.adviceWith(this.context, "success", AdviceWithRouteBuilder::mockEndpoints);

        // Create the payload.
        final JsonObject cloudEvent = CloudEventTestHelper.buildTestCloudEvent();

        final Exchange exchange = createExchangeWithBody(cloudEvent.encode());

        final CloudEventDecoder cloudEventDecoder = new CloudEventDecoder();

        // Call the decoder to mimic what the exchange will look like when the
        // SplunkIntegration route receives the payload.
        cloudEventDecoder.process(exchange);

        final MockEndpoint sedaEndpoint = getMockEndpoint("mock:seda:push");
        sedaEndpoint.expectedMessageCount(1);

        // Send the exchange to the handler under test.
        this.template.send("direct:handler", exchange);

        MockEndpoint.assertIsSatisfied(1000, TimeUnit.MILLISECONDS, sedaEndpoint);

        final List<Exchange> exchangeList = sedaEndpoint.getExchanges();
        Assertions.assertEquals(1, exchangeList.size(), "only one exchange should have been received");

        final Exchange receivedExchange = exchangeList.get(0);
        final Message receivedMessage = receivedExchange.getIn();

        // Assert that the basic authentication header is correct.
        final Map<String, Object> headers = receivedMessage.getHeaders();
        final String basicAuthValue = (String) headers.get("Authorization");

        Assertions.assertNotNull(basicAuthValue, "the basic authentication should have been set by a processor");

        final String basicAuthUserPass
                = String.format("rh_insights_integration:%s", CloudEventTestHelper.FIELD_NOTIF_METADATA_X_INSIGHT_TOKEN_VALUE);
        final String expectedBase64EncodedValue
                = Base64.getEncoder().encodeToString(basicAuthUserPass.getBytes(Charset.defaultCharset()));

        Assertions.assertEquals(String.format("Basic %s", expectedBase64EncodedValue), basicAuthValue,
                "the basic authentication header contains an unexpected value");

        // Assert that the sent cloud event is the expected one.
        final String body = receivedMessage.getBody(String.class);
        CloudEventTestHelper.assertEventIsTheExpectedOne(new JsonObject(body));
    }
}
