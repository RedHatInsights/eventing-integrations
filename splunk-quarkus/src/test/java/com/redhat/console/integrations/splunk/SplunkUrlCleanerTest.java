package com.redhat.console.integrations.splunk;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.camel.Exchange;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.Test;

import static com.redhat.console.integrations.splunk.SplunkUrlCleaner.SERVICES_COLLECTOR;
import static com.redhat.console.integrations.splunk.SplunkUrlCleaner.SERVICES_COLLECTOR_EVENT;
import static com.redhat.console.integrations.splunk.SplunkUrlCleaner.TARGET_URL;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class SplunkUrlCleanerTest extends CamelQuarkusTestSupport {

    private static final String VALID_TARGET_URL = "https://i.am.valid.com";

    @Test
    void shouldNotChangeUrlWhenPathIsOk() throws Exception {

        Exchange exchange = createExchangeWithBody("I am not used in this test!");
        exchange.setProperty(TARGET_URL, VALID_TARGET_URL);

        new SplunkUrlCleaner().process(exchange);

        assertEquals(VALID_TARGET_URL, exchange.getProperty(TARGET_URL, String.class));
    }

    @Test
    void shouldRemoveServicesCollectorPath() throws Exception {

        Exchange exchange = createExchangeWithBody("I am not used in this test");
        exchange.setProperty(TARGET_URL, VALID_TARGET_URL + SERVICES_COLLECTOR);

        new SplunkUrlCleaner().process(exchange);

        assertEquals(VALID_TARGET_URL, exchange.getProperty(TARGET_URL, String.class));
    }

    @Test
    void shouldRemoveServicesCollectorEventPath() throws Exception {

        Exchange exchange = createExchangeWithBody("I am not used in this test");
        exchange.setProperty(TARGET_URL, VALID_TARGET_URL + SERVICES_COLLECTOR_EVENT);

        new SplunkUrlCleaner().process(exchange);

        assertEquals(VALID_TARGET_URL, exchange.getProperty(TARGET_URL, String.class));
    }
}
