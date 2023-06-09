package com.redhat.console.integrations.splunk;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Org admins may create Splunk integrations with or without the '/services/collector/event' path in the Splunk URL.
 * Since we append the '/services/collector/event' path to all Splunk URLs, this processor removes any unwanted URL
 * suffix.
 */
public class SplunkUrlCleaner implements Processor {

    public static final String TARGET_URL = "targetUrl";
    public static final String SERVICES_COLLECTOR = "/services/collector";
    public static final String SERVICES_COLLECTOR_EVENT = SERVICES_COLLECTOR + "/event";

    @Override
    public void process(Exchange exchange) throws Exception {

        String targetUrl = exchange.getProperty(TARGET_URL, String.class);
        if (targetUrl != null) {
            if (targetUrl.endsWith(SERVICES_COLLECTOR)) {
                String newTargetUrl = targetUrl.substring(0, targetUrl.length() - SERVICES_COLLECTOR.length());
                exchange.setProperty(TARGET_URL, newTargetUrl);
            } else if (targetUrl.endsWith(SERVICES_COLLECTOR_EVENT)) {
                String newTargetUrl = targetUrl.substring(0, targetUrl.length() - SERVICES_COLLECTOR_EVENT.length());
                exchange.setProperty(TARGET_URL, newTargetUrl);
            }
        }
    }
}
