Frank!Framework
===============
**Exchange, modify and aggregate messages between systems!**

![frank-framework-github-banner](frank-framework-github-banner.png)

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/ibissource/iaf/blob/master/LICENSE)
[![Core Tests](https://github.com/ibissource/iaf/workflows/Java%20CI%20with%20Maven/badge.svg)](https://github.com/ibissource/iaf/actions?query=workflow%3A%22Java+CI+with+Maven%22+branch%3Amaster)
[![Pull Requests](https://img.shields.io/github/commit-activity/m/ibissource/iaf?label=Pull%20Requests)](https://github.com/ibissource/iaf/pulls)
[![codecov](https://codecov.io/gh/ibissource/iaf/branch/master/graph/badge.svg)](https://codecov.io/gh/ibissource/iaf)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/56a982cc39084043b2e283a146206ec9)](https://www.codacy.com/gh/ibissource/iaf/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ibissource/iaf&amp;utm_campaign=Badge_Grade)
[![CodeFactor](https://www.codefactor.io/repository/github/ibissource/iaf/badge)](https://www.codefactor.io/repository/github/ibissource/iaf)
[![total GitHub contributors](https://img.shields.io/github/contributors-anon/ibissource/iaf.svg)](https://github.com/ibissource/iaf/graphs/contributors)
[![Maven Central](https://img.shields.io/maven-central/v/org.ibissource/ibis-adapterframework-parent.svg?label=Maven%20Central)](https://central.sonatype.com/search?namespace=org.ibissource&q=adapterframework)
[![Latest Snapshot](https://img.shields.io/nexus/public/org.ibissource/ibis-adapterframework-core?label=Latest%20Snapshot&server=https%3A%2F%2Fnexus.frankframework.org%2F)](https://nexus.frankframework.org/#browse/browse)
[![Public Vulnerabilities](https://img.shields.io/endpoint?url=https%3A%2F%2Fapi-hooks.soos.io%2Fapi%2Fshieldsio-badges%3FbadgeType%3DVulnerabilities%26pid%3D68wlxudjy%26packageVersion%3Dlatest-alpha)](https://app.soos.io/research/packages/Java/org.ibissource/ibis-adapterframework-parent)


## Open-Source, Low-Code & Stateless
The Frank!Framework is a framework that is completely configurable through XML configurations. Each Frank!Application may contain multiple configurations, and each configuration can consist of multiple end-to-end connections which we call 'adapters'. Configurations may be (re)loaded conditionally or individiually for optimal performance and customizability.
The application may be managed and monitored through a web interface or REST API.
See it in action: https://frank2example.frankframework.org

## Running the Frank-Framework 
The Frank!Framework can run on any java runtime, so you have your choice of application server. In our CI we test every PR and Release against Tomcat, Websphere, Wildfly and JBoss, all these aplications servers may be used in production environments.
You may [create containers](/docker/README.md) to run the framework using the beforementioned application servers. Please note that they are for development use only, more info about using and creating them can be found in [Docker.md](Docker.md).

All production-ready containers will be pushed to our [Nexus Repository Manager](https://nexus.frankframework.org/) `frankframework-docker` repository. HELM charts are available [here](https://github.com/ibissource/charts/tree/master/charts/frank-framework).


## Rebranding
The Ibis Adapter Framework has been renamed to "Frank!Framework". The migration is a work in progress, which is why you may encounter some old(er) names throughout our source code. Don't worry, everything will remain fully backwards compatible!

## Releases
All our releases can be found on Maven central. Individual builds can be found on our Nexus repository [here](https://nexus.frankframework.org).
For more information about our releases (such as improvements, non-backwards compatibility changes and security fixes), see the release notes of your version [here](https://github.com/ibissource/iaf/releases).

## Security
It is important to remember that the security of your Frank!Application is the result of the overall security of the hosting stack; the Java runtime, Application Server, Frank!Framework and your configuration.

It is our responsibility that there are no vulnerabilities in the Frank!Framework itself and all it's Java dependencies. In turn it is your responsibility to keep your Frank!Framework version up to date and ensure there are no vulnerabilities in your configuration.
More information about reporting vulnerabilities, supported versions and how we deal with CVE's can be found in our [Security Policy](SECURITY.md).

## Feedback
For bug reports and feature requests, create a new issue at <https://github.com/ibissource/iaf/issues>. 
For general questions feel free to post them on our [discussions forum](https://github.com/ibissource/iaf/discussions) here on GitHub. 
If you would like to report a vulnerability, or have security concerns regarding the Frank!Framework, please email security@frankframework.org and include the word "SECURITY" in the subject line.

## Frank!Manual
In need of help? Our manual can be found at <http://frank-manual.readthedocs.io>. If you cannot find an answer to your question [feel free to contact us](https://wearefrank.nl/en/contact/). If you want to contribute to our manual, the sources can be found [here](https://github.com/ibissource/frank-manual).

## Contributing
Eager to help us expand or enhance our framework? 
Please read our [Code of Conduct](CODE_OF_CONDUCT.md) before [Contributing](CONTRIBUTING.md).
