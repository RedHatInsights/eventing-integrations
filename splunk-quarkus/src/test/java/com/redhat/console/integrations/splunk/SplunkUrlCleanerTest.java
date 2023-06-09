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
    void testMultipleUrls() throws Exception {

        /*
         * For some reason, running multiple @Test methods in the same class leads to an UnsupportedOperationException.
         * The following testing code is an ugly workaround: multiple things are tested in the same @Test.
         * This is all temporary as this code will be moved and refactored entirely very soon.
         */

        // Valid URL

        Exchange exchange = createExchangeWithBody("I am not used in this test!");
        exchange.setProperty(TARGET_URL, VALID_TARGET_URL);

        new SplunkUrlCleaner().process(exchange);

        assertEquals(VALID_TARGET_URL, exchange.getProperty(TARGET_URL, String.class));

        // '/services/collector' path suffix

        exchange = createExchangeWithBody("I am not used in this test");
        exchange.setProperty(TARGET_URL, VALID_TARGET_URL + "/services/collector");

        new SplunkUrlCleaner().process(exchange);

        assertEquals(VALID_TARGET_URL, exchange.getProperty(TARGET_URL, String.class));

        // '/services/collector/event' path suffix

        exchange = createExchangeWithBody("I am not used in this test");
        exchange.setProperty(TARGET_URL, VALID_TARGET_URL + "/services/collector/event");

        new SplunkUrlCleaner().process(exchange);

        assertEquals(VALID_TARGET_URL, exchange.getProperty(TARGET_URL, String.class));
    }
}
