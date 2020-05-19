[<img src="https://sling.apache.org/res/logos/sling.png"/>](https://sling.apache.org)

 [![Build Status](https://builds.apache.org/buildStatus/icon?job=Sling/sling-org-apache-sling-jcr-presence/master)](https://builds.apache.org/job/Sling/job/sling-org-apache-sling-jcr-presence/job/master) [![Test Status](https://img.shields.io/jenkins/t/https/builds.apache.org/job/Sling/job/sling-org-apache-sling-jcr-presence/job/master.svg)](https://builds.apache.org/job/Sling/job/sling-org-apache-sling-jcr-presence/job/master/test_results_analyzer/) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/org.apache.sling.jcr.presence/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22org.apache.sling.jcr.presence%22) [![JavaDocs](https://www.javadoc.io/badge/org.apache.sling/org.apache.sling.jcr.presence.svg)](https://www.javadoc.io/doc/org.apache.sling/org.apache.sling.jcr.presence) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Apache Sling JCR Presence

This module is part of the [Apache Sling](https://sling.apache.org) project.

## User Presenter

If an OSGi component depends on a service (system) user, the user presenter can be used to announce the presence of the user as OSGi service.

The OSGi component can reference the service which represents the user, e.g.

    @Reference(
        target = "(&(userId=sling-readall)(disabled=false)(systemUser=true))"
    )
    private UserPresenter userPresenter
