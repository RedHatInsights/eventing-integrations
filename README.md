# Eventing – 3rd-party Integrations

ConsoleDot events integrations with 3rd-party tools (such as Splunk)
build together with [Notifications](https://github.com/RedHatInsights/notifications-backend/).

## Development

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
$ cd splunk-quarkus
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
{"specversion":"1.0","type":"com.redhat.console.notification.toCamel.splunk","source":"notifications","id":"9dc9a4b1-8868-4afc-a69d-e8723b20452c","time":"2022-02-02T14:09:55.532551Z","rh-account":"12345","data":"{\"notif-metadata\":{\"url\":\"http://localhost:8088\",\"X-Insight-Token\":\"TOKEN\",\"extras\":\"{}\"},\"account_id\":\"12345\",\"org_id\":\"67890\",\"application\":\"advisor\",\"bundle\":\"rhel\",\"context\":{},\"event_type\":\"new-recommendation\",\"timestamp\":\"2022-02-02T14:09:55.344612\",\"events\":[{\"some\":\"eventdata\"}]}"}
```
(don't forget to replace `TOKEN` with your HEC token)

Within platform this can be achieved for example using the Drift service:
* registering a system
* creating a baseline out of the registered system
* assigning the system to baseline
* chaning a fact of the baseline (e.g. bios) or changing the system
  (updating a package)
* running a system check-in from the system

### Running unit tests

With local dev mode

```
$ cd splunk-quarkus
$ ACG_CONFIG=./devel.json ../mvnw quarkus:dev -Dquarkus.kafka.devservices.enabled=false
```

running tests are paused.

You can hit `o` to toggle test output and `r` key to resume testing and again `r` to re-run tests.

Alternatively you can run tests via


```
$ ACG_CONFIG=./devel.json ../mvnw quarkus:test -Dquarkus.kafka.devservices.enabled=false
```
and use `r` to re-run tests.


It is also possible to run tests one-time

```
$ ACG_CONFIG=./devel.json ../mvnw test -Dquarkus.kafka.devservices.enabled=false
```


## Development Notes

## Updating Java Dependencies

The best way how to update Java/Quarkus dependencies is to update Quarkus platform version.
The platform version is mapped to a set of dependent packages of Camel provided by a BOM (Bill of Materials)
package [`io.quarkus.platform:quarkus-camel-bom`][quarkus-camel-bom]
(available from [quarkusio/quarkus-platform](https://github.com/quarkusio/quarkus-platform)).

See also
* [Camel Dependency Management](https://camel.apache.org/camel-quarkus/latest/user-guide/dependency-management.html) for Quarkus
* [Quarkus Platform Guide](https://quarkus.io/guides/platform)

### Process

1. Check version of [quarkus.platform.version](/splunk-quarkus/pom.xml) project property whether it matches
   the newest possible version as per [quarkus-camel-bom][quarkus-camel-bom].
   * Note: always use `.Final` versions.
1. Increase the *minor* version of `quarkus.platform.version` project property by one.
1. Read [Quarkus Migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guides)
   for the minor version.
1. Compile code with `mvnw clean compile` (from the corresponding directory) and check for errors.
1. Repeat to increase the minor version up to the highest possible.
1. Test the app, e.g. by running `mvnw quarkus:dev`.
   * Check [how to run Splunk Camel](splunk-quarkus/README.md#running)


[quarkus-camel-bom]: https://search.maven.org/artifact/io.quarkus.platform/quarkus-camel-bom
