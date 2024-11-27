Quick Start
===============

# Building the Frank-Framework
You may either use our pre-build Docker images or deploy it as an EAR or WAR archive.
In this quick start we try to explain on how you can use our project.


## WAR Archive
As a Maven project the easiest way to use our framework is to use one of our starter pom files.
By using either our minimal or full bundle you don't have the overhead of defining the required modules your self, as well as (transient) dependency version 'locks' required for certain application servers.

```
<parent>
	<groupId>org.frankframework</groupId>
	<artifactId>frankframework-bundle-minimal</artifactId>
	<version>${ff.version}</version>
</parent>
```
Or the full bundle which contains almost every module:

```
<parent>
	<groupId>org.frankframework</groupId>
	<artifactId>frankframework-bundle-full</artifactId>
	<version>${ff.version}</version>
</parent>
```
If you wish to use `CMIS` or `Aspose` you will need to manually add those dependencies as such:

```
<dependency>
	<groupId>org.frankframework</groupId>
	<artifactId>frankframework-aspose</artifactId>
	<version>${ff.version}</version>
</dependency>
```
Note:
- The versions of the parent and other framework modules need to match.
- We have example modules such as `test` and `ear` in case you would like some reference material.

## Docker Image
We've tried to keep it simple and incorporate our Docker build files (Dockerfiles) as well as how they are invoked in our Maven poms.
If you want to create custom images to run the framework, or wish to see how we build our own images, see our [docker module](/docker/README.md).


# Running the Frank-Framework
You may choose to use our pre-built Docker images, which you can directly run without having to compile our code.
More info about using and creating containers can be found in [Docker.md](Docker.md).

All production-ready containers will be pushed to our [Nexus Repository Manager](https://nexus.frankframework.org/) `frankframework-docker` repository. Helm charts are available [in the charts repository](https://github.com/frankframework/charts).

## Frank!Manual
In need of help? Our manual can be found at <http://frank-manual.readthedocs.io>. If you cannot find an answer to your question feel free to [submit a question in discussions](https://github.com/frankframework/frankframework/discussions).

## Contributing
Eager to help us expand or enhance our framework?
Please read our [Code of Conduct](CODE_OF_CONDUCT.md) before [Contributing](CONTRIBUTING.md).
