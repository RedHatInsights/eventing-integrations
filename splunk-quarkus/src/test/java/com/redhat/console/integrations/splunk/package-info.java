/**
 * The package contains everything related to the {@link com.redhat.console.integrations.splunk.SplunkIntegration}
 * class. There is only one test per class since otherwise an {@link java.lang.UnsupportedOperationException} is thrown
 * inside the Camel-Quarkus extension, since it apparently tries to shut down the entire context per test, which for
 * some reason makes Quarkus unhappy.
 */
package com.redhat.console.integrations.splunk;
