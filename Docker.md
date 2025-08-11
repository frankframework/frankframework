# Frank!Framework with Docker

Docker images are provided, suitable both for local and server use. Images are provided from the registry located on [DockerHub frankframework/frankframework](https://hub.docker.com/r/frankframework/frankframework), but may only be
available for [6 months](https://www.docker.com/blog/scaling-dockers-business-to-serve-millions-more-developers-storage/), at https://nexus.frankframework.org stable and latest nightly images will be stored for as long as possible.
The source is available from the [docker-folder](docker/Tomcat) in this repository.

## Contents

<!-- TOC -->
* [Frank!Framework with Docker](#frankframework-with-docker)
  * [Contents](#contents)
  * [General use](#general-use)
    * [Local use](#local-use)
    * [Server use](#server-use)
  * [Filesystem](#filesystem)
    * [Directories](#directories)
    * [Files](#files)
  * [Logging](#logging)
  * [Environment variables](#environment-variables)
  * [Health and readiness](#health-and-readiness)
  * [Considerations](#considerations)
    * [HTTPS and authentication](#https-and-authentication)
    * [Secrets](#secrets)
    * [Drivers](#drivers)
    * [Non-root](#non-root)
<!-- TOC -->

## General use

The image contains an empty framework-instance that needs to be configured before use.

Whether using the container locally or building your own image for use on servers, refer to [Filesystem](#Filesystem)
information on which directories and files to mount or copy.

For a list of available tags, see https://hub.docker.com/r/frankframework/frankframework/tags and https://nexus.frankframework.org/#browse/search/docker.

### Local use

To run the image, run the following command, adding environment variables and mounts as needed:

```shell
docker run -p <hostport>:8080 [-e <name>=<value>] [-v <source>:<target>[:<options>]] --name <name> frankframework/frankframework[:<tag>]
```

For example, to run [Frank2Example](https://github.com/frankframework/frankframework/tree/master/example) on http://localhost with the latest image using Powershell on Windows or Bash on
Linux:

```shell
docker run -p 80:8080 \
	-e dtap.stage=LOC \
	-v ./example/src/main/resources:/opt/frank/resources \
	--name Frank2Example \
	frankframework/frankframework:latest
```

Or as Docker Compose service:

```yaml
services:
	frank2example:
		image: frankframework/frankframework:latest
		ports:
			- "80:8080"
		environment:
			- dtap.stage=LOC
		volumes:
			- ./example/src/main/resources:/opt/frank/resources
```

### Server use

Please read the [Considerations](#Considerations) before using the image on servers, as the default setup might not be
secure enough for your use.

For use on servers, you need to build your own image that includes the required configuration files. To start building
your own image, start your Dockerfile with:

`FROM frankframework/frankframework[:<tag>]`

Use `COPY --chown=tomcat` when copying files to ensure that tomcat can use the files.

## Filesystem

### Directories

The image contains the following directories:

| directory                 | description                                                                                                                | notes                                                                                                                                                                                                                          |
|---------------------------|----------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| /opt/frank/configurations | For configurations, may contain a directory with files per configuration or a JAR containing a directory per configuration | When Configuration.xml is not located at `<configurationName>/Configuration.xml`, your resources should include a property `configurations.<configurationName>.configurationFile` containing the path to the Configuration.xml |
| /opt/frank/resources      | For application-wide properties, may contain files or a JAR with all files                                                 | Minimum required properties to set are `instance.name` and `configurations.names`, can also be set using environment variables                                                                                                 |
| /opt/frank/testtool       | For Larva tests that are included in the image                                                                             |                                                                                                                                                                                                                                |
| /opt/frank/testtool-ext   | For Larva tests that are mounted from the environment                                                                      |                                                                                                                                                                                                                                |
| /opt/frank/secrets        | Credential storage (credentials.properties will be read by default)                                                        | See [Secrets](#Secrets)                                                                                                                                                                                                        |
| /opt/frank/drivers        | Contains driver JARs                                                                                                       | See [Drivers](#Drivers)                                                                                                                                                                                                        |
| /usr/local/tomcat/logs    | Log directory                                                                                                              |                                                                                                                                                                                                                                |

### Files

The image also contains the following files:

| file                                       | description                                          | notes                                                                                                                                                                    |
|--------------------------------------------|------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| /opt/frank/resources/resources.yml         | mount/copy of your resources.yml                     | Use hostname `host.docker.internal` to get to the host machine for local testing. Changing this file will require a new instance to be started, it cannot be reloaded    |
| /usr/local/tomcat/conf/server.xml          | mount/copy of your server.xml                        | Contains the default server.xml of Tomcat, replace to secure your application                                                                                            |
| /usr/local/tomcat/conf/catalina.properties | Server properties, contains default framework values | Do not replace this file, use [Environment variables](#Environment-variables) or append to the file, see [Dockerfile](docker/appserver/Tomcat/Dockerfile) for an example |

## Logging

Generated log files are stored in `/usr/local/tomcat/logs`.

In some cases you might want to change the log-appenders, for example to log to stdout. Refer to the [Frank!Framework Manual: Custom Logging](https://frank-manual.readthedocs.io/en/latest/deploying/customLogging.html#custom-logging) for more information.

## Environment variables

Environment variables can be used to set properties. Environment variables have the highest precedence and override
application properties set in .property files supplied by Tomcat, resources and configurations.

Though you probably do not need to touch these file, because of the CredentialProvider, environment variables can also be used to replace properties set inside Tomcat specific configuration files such as `server.xml` and `context.xml`.

Do not use environment variables for secrets! See [Secrets](#Secrets) for more information.

## Health and readiness

The health and readiness of the container can be monitored by polling the `/iaf/api/server/health` API endpoint.
This endpoint will return a HTTP statuscode of 200 if the configurations are loaded and a HTTP statuscode of 503 if there are configurations in a non-running state.
If you want to check the health of the adapters, you can poll the `/iaf/api/configurations/{configuration}/adapters/{name}/health` endpoint.
This endpoint will return a HTTP statuscode of 200 if the adapter is running and a HTTP statuscode of 503 if the adapter is in a non-running state.

## Considerations

The Frank!Framework image is based on Tomcat, all restrictions and considerations that apply to Tomcat also apply to using the provided images.
provided images.

### HTTPS and authentication

The Frank!Framework runs on HTTP by default. It can be secured with a reverse proxy or by overriding the `application.security.http.transportGuarantee` property with a system property.
Tomcat should be configured through the `server.xml` file, which needs to be mounted en overwritten in the container.

### Secrets

Special consideration should be taken with secrets. As described on the [Tomcat website](https://cwiki.apache.org/confluence/display/TOMCAT/Password), secrets are stored in plain text in the container.

The Frank!Framework includes the `CredentialManager Capability`, this allows property substitution in configuration files, like `resources.yml` and in Tomcat settings, such as the `context.xml`.

The default configuration for the `Frank!Framework CredentialManager`, is to load credentials from the `credentials.properties` file.

To use secrets in your Frank!Framework Application configuration, you can take the following steps:

- In your configuration, use the authAlias attribute with value `${<secret-name>}`
- In cases where you need to use username or password separately (such as the Tomcat context.xml), you can set the
  values to `${<secret-name>/username}` and `${<secret-name>/password}` respectively
- Insert the value for the username in the file `/opt/frank/secrets/credentials.properties` as `<secret-name>/username=<username>`
- Insert the value for the password in the file `/opt/frank/secrets/<secret-name>/password` as `<secret-name>/password=<password>`

See the [credentials.properties](credentialProvider/src/test/resources/credentials.properties) of the test-project for an example.

More information on credentials can be found in the [Frank!Framework Manual](https://frank-manual.readthedocs.io/en/latest/deploying/credentials.html#credentials).

### Drivers

Some drivers are included in the image, such as the JDBC and JMS drivers for H2, PostgreSQL and ActiveMQ. If you need to use a different driver or specific version, you can mount the driver JARs to the `/opt/frank/drivers` directory in the container.
This allows you to use your own drivers without having to rebuild the image.

### Non-root

This image runs Tomcat as a separate user `tomcat:tomcat` with `UID=2000` and `GID=2000` instead of `root`.
Keep this in mind when copying or mounting files to the container, as the files need to be owned by `tomcat:tomcat` instead of `root`.
