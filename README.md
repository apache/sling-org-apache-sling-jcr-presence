[![Apache Sling](https://sling.apache.org/res/logos/sling.png)](https://sling.apache.org)

&#32;[![Build Status](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-jcr-presence/job/master/badge/icon)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-jcr-presence/job/master/)&#32;[![Test Status](https://img.shields.io/jenkins/tests.svg?jobUrl=https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-jcr-presence/job/master/)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-jcr-presence/job/master/test/?width=800&height=600)&#32;[![jcr](https://sling.apache.org/badges/group-jcr.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/groups/jcr.md) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Apache Sling JCR Presence

This module is part of the [Apache Sling](https://sling.apache.org) project.

## User Presenter

If an OSGi component depends on a service (system) user, the user presenter can be used to announce the presence of the user as OSGi service.

The OSGi component can reference the service which represents the user, e.g.

    @Reference(
        target = "(&(userId=sling-readall)(disabled=false)(systemUser=true))"
    )
    private UserPresenter userPresenter
