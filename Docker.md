# Frank!Framework with Docker

Docker images are provided, suitable both for local and server use. Images are provided from the registry located at https://nexus.frankframework.org, where images will be stored for as long as possible. 
Specific nightly builds are made available on [DockerHub frankframework/frankframework](https://hub.docker.com/r/frankframework/frankframework), but may only be available for [6 months](https://www.docker.com/blog/scaling-dockers-business-to-serve-millions-more-developers-storage/).
The source is available from the [docker-folder](docker/Tomcat) in this repository.

## Contents

- [General use](#General-use)
  - [Local use](#Local-use)
  - [Server use](#Server-use)
- [Filesystem](#Filesystem)
  - [Directories](#Directories)
  - [Files](#Files)
- [Logging](#Logging)
- [Environment variables](#Environment-variables)
- [Health and readiness](#Health-and-readiness)
- [Considerations](#Considerations)
  - [HTTPS and authentication](#HTTPS-and-authentication)
  - [Secrets](#Secrets)
  - [Non-root](#Non-root)


## General use

The image contains an empty framework-instance that needs to be configured before use.

Whether using the container locally or building your own image for use on servers, refer to [Filesystem](#Filesystem) information on which directories and files to mount or copy.

For a list of available tags, see https://nexus.frankframework.org/#browse/search/docker.

### Local use

To run the image, run the following command, adding environment variables and mounts as needed:

`docker run --publish <hostport>:8080 [-e <name>=<value>] [-v <source>:<target>[:<options>]] --name <name> nexus.frankframework.org/frank-framework[:<tag>]`

For example, to run Frank2Example on http://localhost with the latest image using Powershell on Windows:

```bash
docker run --publish 80:8080 \
	-e dtap.stage=LOC \
	-v $pwd/example/src/main/resources:/opt/frank/resources \
	-v $pwd/example/src/main/webapp/META-INF/context.xml:/usr/local/tomcat/conf/Catalina/localhost/ROOT.xml \
	--name Frank2Example \
	nexus.frankframework.org/frank-framework:latest
```

### Server use

Please read the [Considerations](#Considerations) before using the image on servers, as the default setup might not be secure enough for your use.

For use on servers, you need to build your own image that includes the required configuration files. To start building your own image, start your Dockerfile with:

`FROM nexus.frankframework.org/frank-framework[:<tag>]`

Use `COPY --chown=tomcat` when copying files to ensure that tomcat can use the files.

## Filesystem

### Directories

The image contains the following directories:
| directory | description | notes |
|---|---|---|
| /opt/frank/resources | For application-wide properties, may contain files or a .jar with all files | Minimum required properties to set are `instance.name` and `configurations.names`, can also be set using environment variables |
| /opt/frank/configurations | For configurations, may contain a directory with files per configuration or a .jar containing a directory per configuration | When Configuration.xml is not located at `<configurationName>/Configuration.xml`, your resources should include a property `configurations.<configurationName>.configurationFile` containing the path to the Configuration.xml |
| /opt/frank/testtool | For Larva tests that are included in the image | |
| /opt/frank/testtool-ext | For Larva tests that are mounted from the environment | |
| /usr/local/tomcat/lib | Contains drivers and other dependencies | Contains all Framework required dependencies and drivers for supported JMS and JDBC systems |
| /usr/local/tomcat/logs | Log directory | |
| /opt/frank/secrets | Credential storage | See [Secrets](#Secrets) |

### Files

The image also contains the following files:
| file | description | notes |
|---|---|---|
| /usr/local/tomcat/conf/Catalina/localhost/ROOT.xml | mount/copy of your context.xml | Use hostname `host.docker.internal` to get to the host machine for local testing. Changing this file will require a new instance to be started, it cannot be reloaded |
| /usr/local/tomcat/conf/server.xml | mount/copy of your server.xml | Contains the default server.xml of Tomcat, replace to secure your application |
| /usr/local/tomcat/conf/catalina.properties | Server properties, contains default framework values | Do not replace this file, use [Environment variables](#Environment-variables) or append to the file, see [Dockerfile](docker/appserver/Tomcat/Dockerfile) for an example |

## Logging

Generated log files are stored in `/usr/local/tomcat/logs`.

## Environment variables

Environment variables can be used to set parameters. Environment variables have the highest precedence and override parameters set in .property files supplied by Tomcat, resources and configurations.

Environment variables can be used to replace parameters in Tomcat configuration files such as server.xml and context.xml.

Do not use environment variables for secrets!

## Health and readiness

The health and readiness of the container can be monitored by polling the `/iaf/api/server/health` API endpoint. This will return a HTTP statuscode of 200 if all adapters are running and a HTTP statuscode of 503 if there are adapters in a non-running state.

## Considerations

The images are based on Tomcat, all restrictions and considerations that apply to Tomcat also apply to using the provided images.

### HTTPS and authentication

Frank!Applications use HTTPS and require authentication unless `dtap.stage=LOC`, but the default server.xml of Tomcat is not configured for inbound HTTPS traffic and user authentication. To configure this, the server.xml file will need to be replaced by either building your own image or mounting it at runtime.

### Secrets

Special consideration should be taken with secrets. As described on the [Tomcat website](https://cwiki.apache.org/confluence/display/TOMCAT/Password), secrets are stored in plain text in the container. To use secrets in your Tomcat and Frank!Application configuration, you can take the following steps:
- In your configuration, use the authAlias attribute with value `${<secret-name>}` 
- In cases where you need to use username or password separately (such as the Tomcat context.xml), you can set the values to `${<secret-name>/username}` and `${<secret-name>/password}` respectively
- Mount the value for the username in the file `/opt/frank/secrets/<secret-name>/username`
- Mount the value for the password in the file `/opt/frank/secrets/<secret-name>/password`

See the [context.xml](test/src/main/webapp/META-INF/context.xml) of the test-project and corresponding [Dockerfile](docker/appserver/Tomcat/test/Dockerfile) for an example.

### Non-root

This image runs Tomcat as a separate user `tomcat:tomcat` with `UID=1000` and `GID=1000` instead of `root`. If you need to run as `root`, you will need to set `USER root` in your Dockerfile.
