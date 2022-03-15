package com.redhat.console.notifications.splunkintegration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class WireMockTestResource implements QuarkusTestResourceLifecycleManager {

    private WireMockServer server;

    @Override
    public Map<String, String> start() {
        // Setup & start the server
        server = new WireMockServer(
            wireMockConfig().dynamicPort()
        );
        server.start();

        // Stub a HTTP endpoint. Note that WireMock also supports a record and playback mode
        // http://wiremock.org/docs/record-playback/
        server.stubFor(
            get(urlEqualTo("")) // we don't have an api, so can we just reach out to Notifications logs and check for our success/error responses there via api?  if not, can this instead be hooked up to some sort of ngrok type thing waiting on a 200 for the send to splunk even if we don't hook it to an actual splunk? or can this work in conjunction with the container thing below?
                .willReturn(aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"message\": \"Hello World\"}")));

        // Ensure the camel component API client passes requests through the WireMock proxy
        Map<String, String> conf = new HashMap<>();
        conf.put("camel.component.foo.server-url", server.baseUrl());
        return conf;
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }
}

public class MyTestResource implements QuarkusTestResourceLifecycleManager {

    private GenericContainer myContainer;

    @Override
    public Map<String, String> start() {
        // Start the needed container(s)
        myContainer = new GenericContainer(...)
                .withExposedPorts(8088)
                .waitingFor(Wait.forListeningPort());

        myContainer.start();

        // Pass the configuration to the application under test
        return new HashMap<>() {{
                put("my-container.host", container.getContainerIpAddress());
                put("my-container.port", "" + container.getMappedPort(8088));
        }};
    }

    @Override
    public void stop() {
        // Stop the needed container(s)
        myContainer.stop();
    }
}

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(WireMockTestResource.class)
class EventSendTest {
   // add our test here
   @Test
    public void sendMessageShouldSucceed() {
        RestAssured.get("").then().statusCode(200); // if we can use something like how ngrok gives us a 200 response or some other local mock of our sending
    }
}

@QuarkusTest
class EventLogTest {
    @Test
    void testEventFailLog() {
        await().atMost(10L, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
            String log = new String(Files.readAllBytes(Paths.get("target/quarkus.log")), StandardCharsets.UTF_8);
            return log.contains("Fail with for id");
        });
    }

    @Test
    void testEventSuccessLog() {
        await().atMost(10L, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
            String log = new String(Files.readAllBytes(Paths.get("target/quarkus.log")), StandardCharsets.UTF_8);
            return log.contains("Response ");
        });
    }

}
