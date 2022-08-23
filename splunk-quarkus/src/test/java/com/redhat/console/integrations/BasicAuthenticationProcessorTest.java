package com.redhat.console.integrations;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.test.junit5.ExchangeTestSupport;
import org.apache.camel.util.json.JsonObject;
import org.apache.http.auth.AuthenticationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BasicAuthenticationProcessorTest extends ExchangeTestSupport {
    private final BasicAuthenticationProcessor processor = new BasicAuthenticationProcessor("TestUser");

    @Test
    public void testGenerateAuthorizeBasicAuthHeader() throws AuthenticationException {
        Exchange exchange = createExchange();

        Map<String, String> metadataMap = new LinkedHashMap<String, String>();
        metadataMap.put("X-Insight-Token", "Test|Password:");

        JsonObject metadata = new JsonObject(metadataMap);
        exchange.getIn().setHeader("metadata", metadata);

        processor.process(exchange);

        assertEquals("Basic VGVzdFVzZXI6VGVzdHxQYXNzd29yZDo=", exchange.getIn().getHeader("Authorization", String.class));

    }

    @Test
    public void testGenerateNoAuthorizeOnEmptyToken() throws AuthenticationException {
        Exchange exchange = createExchange();

        Map<String, String> metadataMap = new LinkedHashMap<String, String>();
        metadataMap.put("X-Insight-Token", "");

        JsonObject metadata = new JsonObject(metadataMap);
        exchange.getIn().setHeader("metadata", metadata);

        processor.process(exchange);

        assertNull(exchange.getIn().getHeader("Authorization", String.class));

    }
}
