package com.redhat.console.notifications.splunkintegration;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.ProtocolException;

public class TargetUrlValidator implements Processor {

    public void process(Exchange exchange) throws Exception {
        String[] http_schemes = { "http" };
        String[] https_schemes = { "https" };
        UrlValidator httpUrlValidator = new UrlValidator(http_schemes, UrlValidator.ALLOW_LOCAL_URLS);
        UrlValidator httpsUrlValidator = new UrlValidator(https_schemes, UrlValidator.ALLOW_LOCAL_URLS);

        Message in = exchange.getIn();
        String url = in.getHeader("targetUrl", String.class);

        // we don't support http
        if (httpUrlValidator.isValid(url)) {
            throw new ProtocolException("Insecure protocol is not supported");
        } else if (!httpsUrlValidator.isValid(url)) {
            throw new IllegalArgumentException("URL Validation failed");
        }
    }

}
