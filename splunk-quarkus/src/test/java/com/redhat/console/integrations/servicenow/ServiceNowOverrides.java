package com.redhat.console.integrations.servicenow;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class ServiceNowOverrides implements QuarkusTestProfile {
    /**
     * The "javaRoutesIncludePattern" property must be overridden in order to signal Camel that the "Service Now" routes
     * should be loaded instead.
     * 
     * @return a map with "javaRoutesIncludePattern" overriden.
     */
    public Map<String, String> getConfigOverrides() {
        return Map.of("camel.main.javaRoutesIncludePattern", "**/MainRoutes*,**/ErrorHandlingRoutes*,**/ServiceNow*");
    }
}
