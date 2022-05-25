# Splunk Integration on Quarkus

## Development/Local

### Prerequisites

* podman/docker
* [OpenJDK Development Environment](https://openjdk.java.net/guide/)
  (`java-latest-openjdk-devel` package in Fedora)

### Setup

Set HTTP Event Collector (HEC) on your (local) Splunk and copy the token.
Ensure that the HEC has SSL disabled and that all tokens are enabled.

### Container Image Build

```
$ cd splunk-quarkus
$ podman build -f Dockerfile.jvm -t quay.io/cloudservices/eventing-integrations-splunk ..
```

### Running

Running within container:

```
podman run -it -e ACG_CONFIG=/cdapp/devel.json -v devel.json:/cdapp/devel.json quay.io/cloudservices/eventing-integrations-splunk
```

You might ommit the interative terminal options `-it` if you want to have
it running in background (detached).


To run it locally with dev mode execute:

```
$ ACG_CONFIG=./devel.json ../mvnw quarkus:dev -Dquarkus.kafka.devservices.enabled=false
```

The integration would connect to Kafka defined within `ACG_CONFIG`
listenting on `platform.notifications.tocamel`.


### Trying it out

Generate a message on `platform.notifications.tocamel` Kafka topic.

Manually this can be achieved for example by producing a message using
[`kafka-console-producer.sh`](https://kafka.apache.org/quickstart)
tool from Kafka:
```
kafka-console-producer.sh --bootstrap-server localhost:9092 --topic platform.notifications.tocamel
```

Here is an example CloudEvent for testing:
```
{"specversion":"1.0","type":"com.redhat.console.notification.toCamel.splunk","source":"notifications","id":"9dc9a4b1-8868-4afc-a69d-e8723b20452c","time":"2022-02-02T14:09:55.532551Z","rh-account":"12345","data":"{\"notif-metadata\":{\"url\":\"http://localhost:8088\",\"X-Insight-Token\":\"TOKEN\",\"extras\":\"{}\"},\"account_id\":\"12345\",\"application\":\"advisor\",\"bundle\":\"rhel\",\"context\":{},\"event_type\":\"new-recommendation\",\"timestamp\":\"2022-02-02T14:09:55.344612\",\"events\":[{\"some\":\"eventdata\"}]}"}
```
(don't forget to replace `TOKEN` with your HEC token)

Within platform this can be achieved for example using the Drift service:
* registering a system
* creating a baseline out of the registered system
* assigning the system to baseline
* chaning a fact of the baseline (e.g. bios) or changing the system
  (updating a package)
* running a system check-in from the system

