package com.redhat.console.notifications.splunkintegration;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.commons.validator.routines.UrlValidator;

public class TargetUrlValidator implements Processor {

    public void process(Exchange exchange) throws Exception {
        String[] schemes = { "http", "https" };
        UrlValidator urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS);

        Message in = exchange.getIn();
        String url = in.getHeader("targetUrl", String.class);
        if (!urlValidator.isValid(url)) {
            throw new IllegalArgumentException("URL Validation failed");
        }
    }

}
