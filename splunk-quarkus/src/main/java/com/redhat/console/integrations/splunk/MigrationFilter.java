package com.redhat.console.integrations.splunk;

import io.quarkus.logging.Log;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MigrationFilter implements Predicate {

    public static final String KAFKA_PROCESSOR = "kafkaProcessor";

    @Override
    public boolean matches(Exchange exchange) {
        String kafkaProcessor = exchange.getProperty(KAFKA_PROCESSOR, String.class);
        if (!"connector".equals(kafkaProcessor)) {
            return true;
        } else {
            Log.info("Kafka message ignored because it is marked to be processed by the Notifications connector");
            return false;
        }
    }
}
