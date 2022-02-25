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
$ podman build -f Dockerfile.jvm -t quay.io/vkrizan/eventing-splunk-quarkus ..
```

### Running

Running within container:

```
podman run -it -e ACG_CONFIG=/cdapp/devel.json -v devel.json:/cdapp/devel.json quay.io/vkrizan/eventing-splunk-quarkus
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
{"data":"{\"notif-metadata\":{\"extras\":\"{\\\"token\\\":\\\"TOKEN\\\"}\",\"url\":\"localhost:8088\"},\"payload\":\"{}\"}","type":"com.redhat.console.notification.toCamel.splunk"}
```
(don't forget to replace `TOKEN` with your HEC token)

Within platform this can be achieved for example using the Drift service:
* registering a system
* creating a baseline out of the registered system
* assigning the system to baseline
* chaning a fact of the baseline (e.g. bios) or changing the system
  (updating a package)
* running a system check-in from the system

