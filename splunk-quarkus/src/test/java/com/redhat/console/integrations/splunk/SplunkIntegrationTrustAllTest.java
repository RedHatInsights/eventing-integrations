package com.redhat.console.integrations.splunk;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.redhat.console.integrations.CloudEventDecoder;
import com.redhat.console.integrations.testhelpers.CloudEventTestHelper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.callback.QuarkusTestContext;
import io.vertx.core.json.JsonObject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

@QuarkusTest
@TestProfile(SplunkIntegrationTrustAllTest.class)
public class SplunkIntegrationTrustAllTest extends CamelQuarkusTestSupport {

    private WireMockServer wireMockServer;

    /**
     * Sets up the WireMock server. This is done this way instead of using a QuarkusTestResource, because for some
     * reason when using the latter and running the entire test suite, Apache Camel returns a "remote host closed
     * connection during handshake" error. By setting up a few logging statements, we saw that the Wiremock server is
     * started/stopped multiple times, even though initially there was only one test that had the QuarkusTestResource
     * annotation.
     */
    @Override
    protected void doAfterConstruct() {
        final WireMockConfiguration wireMockConfiguration = new WireMockConfiguration();
        wireMockConfiguration.httpDisabled(true);
        wireMockConfiguration.httpsPort(8443);

        this.wireMockServer = new WireMockServer(wireMockConfiguration);
        this.wireMockServer.start();

        // Mock Splunk's URL.
        this.wireMockServer.stubFor(
                post(urlEqualTo("/services/collector/event"))
                        .willReturn(aResponse().withStatus(HttpStatus.SC_OK)));
    }

    /**
     * Stop the WireMock server after all the test is over.
     * 
     * @param quarkusTestContext Quarkus' test context.
     */
    @Override
    protected void doAfterAll(final QuarkusTestContext quarkusTestContext) {
        this.wireMockServer.stop();
    }

    /**
     * Tests that under normal circumstances, and when the "trustall" parameter is specified in the payload, then the
     * payload is sent to Splunk without performing an SSL chain verification check.
     * 
     * @throws Exception if any unexpected error occurs.
     */
    @Test
    void testTrustAll() throws Exception {
        // Create the payload.
        final JsonObject cloudEvent = CloudEventTestHelper.buildTestCloudEvent();
        final JsonObject data = cloudEvent.getJsonObject(CloudEventTestHelper.FIELD_DATA);

        // Update the "url" field and set Wiremock's URL. Also, set the
        // "trustall" field to "true" so that Apache Camel doesn't perform the
        // SSL check.
        JsonObject notifMetadata = data.getJsonObject(CloudEventTestHelper.FIELD_NOTIF_METADATA);
        notifMetadata.put(CloudEventTestHelper.FIELD_NOTIF_METADATA_URL, this.wireMockServer.baseUrl());
        notifMetadata.put(CloudEventTestHelper.FIELD_NOTIF_METADATA_TRUST_ALL, true);

        final Exchange exchange = createExchangeWithBody(cloudEvent.encode());

        final CloudEventDecoder cloudEventDecoder = new CloudEventDecoder();

        // Call the decoder to mimic what the exchange will look like when the
        // SplunkIntegration route receives the payload.
        cloudEventDecoder.process(exchange);

        this.template.send("direct:handler", exchange);

        // Wait for the payload to be received.
        final NotifyBuilder notification = new NotifyBuilder(this.context)
                .from("direct:handler")
                .whenDone(1)
                .create();

        notification.matches(10, TimeUnit.SECONDS);

        final List<ServeEvent> receivedRequests = this.wireMockServer.getAllServeEvents();
        Assertions.assertEquals(1, receivedRequests.size(),
                "a single request should have been received in the WireMock server");

        final ServeEvent serveEvent = receivedRequests.get(0);
        final LoggedRequest request = serveEvent.getRequest();

        // Enlist the expected headers to compare them to the ones received in
        // the mock server.
        final Set<String> expectedHeaders = Set.of(
                "Accept-Encoding",
                "Authorization",
                "Connection",
                "Content-Length",
                "Content-Type",
                "Host",
                "User-Agent");

        final HttpHeaders headers = request.getHeaders();

        Assertions.assertEquals(
                expectedHeaders.size(),
                headers.size(),
                String.format("unexpected number of headers received. Want '%s', got '%s'", expectedHeaders, headers.all()));

        // Compare that all the expected headers are present, and that the
        // authorization header contains the expected payload.
        for (final HttpHeader header : request.getHeaders().all()) {
            if (!expectedHeaders.contains(header.key())) {
                Assertions.fail(String.format("unexpected header received: %s", header));
            }

            if (header.keyEquals("Authorization")) {
                Assertions.assertEquals(
                        String.format("Splunk %s", CloudEventTestHelper.FIELD_NOTIF_METADATA_X_INSIGHT_TOKEN_VALUE),
                        header.firstValue(),
                        "the authorization header contained an incorrect value");
            }
        }

        final String rawBody = new String(request.getBody(), StandardCharsets.UTF_8);
        final JsonObject body = new JsonObject(rawBody);

        // Assert the hard coded fields from the payload.
        Assertions.assertEquals("eventing", body.getString("source"));
        Assertions.assertEquals("Insights event", body.getString("sourcetype"));

        // Assert the inner event itself.
        CloudEventTestHelper.assertEventIsTheExpectedOne(body.getJsonObject("event"));
    }
}
