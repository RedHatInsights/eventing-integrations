package com.redhat.console.notifications.splunkintegration;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.apache.camel.Exchange;
import org.apache.camel.test.junit5.ExchangeTestSupport;

public class TargetUrlValidatorTest extends ExchangeTestSupport {
    private final TargetUrlValidator targetUrlValidator = new TargetUrlValidator(); 

    @Test
    // we support http protocol
    public void testValidTargetUrlHttp() {
        Exchange exchange = createExchange();
        String url = "http://example.com/foo?bar=baz";
        exchange.getIn().setHeader("targetUrl", url);
        assertDoesNotThrow(() -> { targetUrlValidator.process(exchange); } );
    }

    @Test
    // we support https protocol
    public void testValidTargetUrlHttps() {
        Exchange exchange = createExchange();
        String url = "https://example.com/foo?bar=baz";
        exchange.getIn().setHeader("targetUrl", url);
        assertDoesNotThrow(() -> { targetUrlValidator.process(exchange); } );
    }

    @Test
    // we don't support ftp protocol
    public void testValidTargetUrlFtp() {
        Exchange exchange = createExchange();
        String url = "ftp://example.com/foo?bar=baz";
        exchange.getIn().setHeader("targetUrl", url);
        assertThrows(IllegalArgumentException.class, () -> { targetUrlValidator.process(exchange); } );
    }

    @Test
    // we don't support non-sense as protocol
    public void testValidTargetUrlGibberish() {
        Exchange exchange = createExchange();
        String url = "foo-bar_baz";
        exchange.getIn().setHeader("targetUrl", url);
        assertThrows(IllegalArgumentException.class, () -> { targetUrlValidator.process(exchange); } );
    }

    @Test
    // we don't support empty string as protocol
    public void testValidTargetUrlEmptyString() {
        Exchange exchange = createExchange();
        String url = "";
        exchange.getIn().setHeader("targetUrl", url);
        assertThrows(IllegalArgumentException.class, () -> { targetUrlValidator.process(exchange); } );
    }

    @Test
    // we don't support null as protocol
    public void testValidTargetUrlNull() {
        Exchange exchange = createExchange();
        String url = null;
        exchange.getIn().setHeader("targetUrl", url);
        assertThrows(IllegalArgumentException.class, () -> { targetUrlValidator.process(exchange); } );
    }

}


