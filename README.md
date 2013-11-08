# Jenkins Node Iterator API Plugin

This plugin provides support for iterating through all the Node instances that are in use by Jenkins,
     even those Node instances that are not traditionally attached to Jenkins. The API exposed by this
     plugin can be used by cloud provider plugins to identify unused provisioned resource.

See also this [plugin's wiki page][wiki]

# Environment

The following build environment is required to build this plugin

* `java-1.6` and `maven-3.0.5`

# Build

To build the plugin locally:

    mvn clean verify

# Release

To release the plugin:

    mvn release:prepare release:perform -B

# Test local instance

To test in a local Jenkins instance

    mvn hpi:run

  [wiki]: http://wiki.jenkins-ci.org/display/JENKINS/Node+Iterator+API+Plugin
