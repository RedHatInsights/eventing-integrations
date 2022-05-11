# Eventing – 3rd-party Integrations

ConsoleDot events integrations with 3rd-party tools (such as Splunk)
build together with [Notifications](https://github.com/RedHatInsights/notifications-backend/).

## Integrations

* [Splunk Camel](splunk-quarkus/README.md)

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
