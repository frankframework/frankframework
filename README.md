Frank!Framework
===============
**Exchange, modify and aggregate messages between systems!**

![frank-framework-github-banner](frank-framework-github-banner.png)

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/frankframework/frankframework/blob/master/LICENSE)
[![Core Tests](https://github.com/frankframework/frankframework/workflows/Java%20CI%20with%20Maven/badge.svg)](https://github.com/frankframework/frankframework/actions?query=workflow%3A%22Java+CI+with+Maven%22+branch%3Amaster)
[![Pull Requests](https://img.shields.io/github/commit-activity/m/frankframework/frankframework?label=Pull%20Requests)](https://github.com/frankframework/frankframework/pulls)
[![codecov](https://codecov.io/gh/frankframework/frankframework/branch/master/graph/badge.svg)](https://codecov.io/gh/frankframework/frankframework)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/99f16cffc31a422589303aed68e7cf98)](https://app.codacy.com/gh/frankframework/frankframework/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![CodeFactor](https://www.codefactor.io/repository/github/frankframework/frankframework/badge)](https://www.codefactor.io/repository/github/frankframework/frankframework)
[![total GitHub contributors](https://img.shields.io/github/contributors-anon/frankframework/frankframework.svg)](https://github.com/frankframework/frankframework/graphs/contributors)
[![Maven Central](https://img.shields.io/maven-central/v/org.frankframework/frankframework-parent.svg?label=Maven%20Central)](https://central.sonatype.com/namespace/org.frankframework)
[![Latest Snapshot](https://img.shields.io/nexus/public/org.frankframework/frankframework-core?label=Latest%20Snapshot&server=https%3A%2F%2Fnexus.frankframework.org%2F)](https://nexus.frankframework.org/#browse/browse)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Ffrankframework%2Ffrankframework.svg?type=shield&issueType=license)](https://app.fossa.com/projects/git%2Bgithub.com%2Ffrankframework%2Ffrankframework?ref=badge_shield&issueType=license)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Ffrankframework%2Ffrankframework.svg?type=shield&issueType=security)](https://app.fossa.com/projects/git%2Bgithub.com%2Ffrankframework%2Ffrankframework?ref=badge_shield&issueType=security)
[![Dependency Vulnerabilities](https://img.shields.io/endpoint?url=https%3A%2F%2Fapi-hooks.soos.io%2Fapi%2Fshieldsio-badges%3FbadgeType%3DDependencyVulnerabilities%26pid%3Dxbuzi9hbw%26)](https://app.soos.io/research/repositories/github/frankframework/frankframework?attributionFormat=soosissues)


## Open-Source, Low-Code & Stateless
The Frank!Framework is a framework that is completely configurable through XML configurations. Each Frank!Application may contain multiple configurations, and each configuration can consist of multiple end-to-end connections which we call 'adapters'. Configurations may be (re)loaded conditionally or individually for optimal performance and customizability.
The application may be managed and monitored through a web interface or REST API.
See it in action: https://frank2example.frankframework.org

## Running the Frank-Framework 
The Frank!Framework can run on any java runtime, so you have your choice of application server. In our CI we test every PR and Release against Tomcat, Wildfly and JBoss, all these application servers may be used in production environments.
You may [create containers](/docker/README.md) to run the framework using the beforementioned application servers. Please note that they are for development use only, more info about using and creating them can be found in [Docker.md](Docker.md).

All production-ready containers will be pushed to our [Nexus Repository Manager](https://nexus.frankframework.org/) `frankframework-docker` repository. Helm charts are available [in the charts repository](https://github.com/frankframework/charts).


## Rebranding
The Ibis Adapter Framework has been renamed to "Frank!Framework". The migration is a work in progress, which is why you may encounter some old(er) names throughout our source code. Don't worry, everything will remain fully backwards compatible!

## Releases
All our releases can be found on Maven central. Individual builds can be found on our Nexus repository [here](https://nexus.frankframework.org).
For more information about our releases (such as improvements, non-backwards compatibility changes and security fixes), see the release notes of your version [here](https://github.com/frankframework/frankframework/releases).

## Security
It is important to remember that the security of your Frank!Application is the result of the overall security of the hosting stack; the Java runtime, Application Server, Frank!Framework and your configuration.

It is our responsibility that there are no vulnerabilities in the Frank!Framework itself and all it's Java dependencies. In turn it is your responsibility to keep your Frank!Framework version up to date and ensure there are no vulnerabilities in your configuration.
More information about reporting vulnerabilities, supported versions and how we deal with CVE's can be found in our [Security Policy](SECURITY.md).

## Feedback
For bug reports and feature requests, create a new issue at <https://github.com/frankframework/frankframework/issues>. 
For general questions feel free to post them on our [discussions forum](https://github.com/frankframework/frankframework/discussions) here on GitHub. 
If you would like to report a vulnerability, or have security concerns regarding the Frank!Framework, please email security@frankframework.org and include the word "SECURITY" in the subject line.

## Frank!Manual
In need of help? Our manual can be found at <http://frank-manual.readthedocs.io>. If you cannot find an answer to your question feel free to [submit a question in discussions](https://github.com/frankframework/frankframework/discussions). If you want to contribute to our manual, the sources can be found [here](https://github.com/frankframework/frank-manual).

## Contributing
Eager to help us expand or enhance our framework? 
Please read our [Code of Conduct](CODE_OF_CONDUCT.md) before [Contributing](CONTRIBUTING.md).
