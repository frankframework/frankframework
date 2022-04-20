Frank!Framework with Docker
===========================

Docker images are provided, suitable both for local and server use. Images are provided from the registry located at <https://TBD>, the source is avalaible from the [docker-folder](docker/appserver/Tomcat) in this repository.

# Contents

- [General use](#General-use)
  - [Local use](#Local-use)
  - [Server use](#Server-use)
- [Filesystem](#Filesystem)
  - [Directories](#Directories)
  - [Files](#Files)
  - [Permissions](#Permissions)
- [Logging](#Logging)
- [Environment variables](#Environment-variables)
- [Health and readiness](#Health-and-readiness)
- [Considerations](#Considerations)
  - [HTTPS and authentication](#HTTPS-and-authentication)
  - [Secrets](#Secrets)
  - [Non-root](#Non-root)


General use
===========
The image contains an empty framework-instance that needs to be configured before use.

Whether using the container locally or building your own image for use on servers, refer to [Filesystem](#Filesystem) information on which directories and files to mount or copy.

For a list of available tags, see https://nexus.frankframework.org/#browse/search/docker.

## Local use

To run the image, run the following command, adding environment variables and mounts as needed:

`docker run --publish <hostport>:8080 [-e <name>=<value>] [--mount type=bind,source=<source>,target=<target>] --name <name> nexus.frankframework.org/frank-framework[:<tag>]`

## Server use

Please read the [Considerations](#Considerations) before using the image on servers, as the default setup might not be secure enough for your use.

For use on servers, you need to build your own image that includes the required configuration files. To start building your own image, start your Dockerfile with:

`FROM nexus.frankframework.org/frank-framework[:<tag>]`

Dockerfiles based on our image use `root` during build. During startup we use `gosu` to step down to a more restricted `tomcat` user.

Filesystem
==========

## Directories

The image contains the following directories:
| directory | description | notes |
|---|---|---|
| /opt/frank/resources | For application-wide properties, may contain files or a .jar with all files | Minimum required properties to set are `instance.name` and `configurations.names`, can also be set using environment variables |
| /opt/frank/configurations | For configurations, may contain a directory with files per configuration or a .jar containing a directory per configuration | When Configuration.xml is not located at `<configurationName>/Configuration.xml`, your resources should include a property `configurations.<configurationName>.configurationFile` containing the path to the Configuration.xml |
| /opt/frank/testtool | For Larva tests that are included in the image | |
| /opt/frank/testtool-ext | For Larva tests that are mounted from the environment | |
| /usr/local/tomcat/lib | Contains drivers and other dependencies | Contains all Framework required dependencies and drivers for supported JMS and JDBC systems |
| /usr/local/tomcat/logs | Log directory | |

## Files

The image also contains the following files:
| file | description | notes |
|---|---|---|
| /usr/local/tomcat/conf/Catalina/localhost/iaf.xml | mount/copy of your context.xml | Use hostname `host.docker.internal` to get to the host machine for local testing. Changing this file will require a new instance to be started, it cannot be reloaded |
| /usr/local/tomcat/conf/server.xml | mount/copy of your server.xml | Contains the default server.xml of Tomcat, replace to secure your application |
| /usr/local/tomcat/conf/catalina.properties | Server properties, contains default framework values | Do not replace this file, use [Environment variables](#Environment-variables) or append to the file, see [Dockerfile](docker/appserver/Tomcat/Dockerfile) for an example |

## Permissions

As this image does not run Tomcat using the root user, file permissions need to be set correctly. By default this is done during startup, to ensure that all files copied to the above locations have the correct permissions. For images with a large number of files in these locations, this can reduce startup performance. It is possible to set the permissions during build and disable the step during startup by adding the following lines at the end of your Dockerfile:
```
RUN /setPermissions.sh
ENV SET_PERMISSIONS_ON_STARTUP=FALSE
```

Logging
=======

Generated log files are stored in `/usr/local/tomcat/logs`.

Environment variables
=====================

Environment variables can be used to set parameters. Environment variables have the highest precedence and override parameters set in .property files supplied by Tomcat, resources and configurations.

As `org.apache.tomcat.util.digester.PROPERTY_SOURCE=org.apache.tomcat.util.digester.EnvironmentPropertySource` is set in our images, environment variables can also be used to replace parameters in Tomcat configuration files such as server.xml and context.xml.

Health and readiness
====================

The health and readiness of the container can be monitored by polling the `/iaf/api/server/health` API endpoint. This will return a HTTP statuscode of 200 if all adapters are running and a HTTP statuscode of 503 if there are adapters in a non-running state.

Considerations
==============

The images are based on Tomcat, all restrictions and considerations that apply to Tomcat also apply to using the provided images.

## HTTPS and authentication

By default, the image uses the default server.xml of Tomcat which is not configured for inbound HTTPS traffic and user authentication on the administration console. To secure your application, replace server.xml with a secured version matching your requirements.

Note: The GUI will not load data if accessed via HTTP and `dtap.stage!=LOC`, HTTP-based listeners will still process messages.

## Secrets

Special consideration should be taken with secrets. As described on the [Tomcat website](https://cwiki.apache.org/confluence/display/TOMCAT/Password), passwords are stored in plain text. To use secrets in your Tomcat and Frank!Application configuration, you can take the following steps:
- In your configuration, refer to the username as `${<secret-name>/username}` and password as `${<secret-name>/password}`
- Mount the username in `/opt/frank/secrets/<secret-name>/username`
- Mount the password in `/opt/frank/secrets/<secret-name>/password`

See the [context.xml](test/src/main/webapp/META-INF/context.xml) of the test-project and corresponding [Dockerfile](docker/appserver/Tomcat/test/Dockerfile) for an example.

## Non-root

This image runs Tomcat as a separate user `tomcat:tomcat` with `UID=1000` and `GID=1000`. To ensure correct file permissions, by default the root user sets the file permissions on startup after which Tomcat is started using `gosu` to step down to `tomcat`. For setups with a large number of files, setting the permissions reduces startup performance, see [Permissions](#Permissions) to set the file permissions during build and skip the step during container startup.

These actions are handled by the [/entrypoint.sh](docker/appserver/Tomcat/src/entrypoint.sh) and [/setPermissions.sh](docker/appserver/Tomcat/src/setPermissions.sh) scripts, replacing or modifying these scripts or changing the ENTRYPOINT of the image might result in incorrect file permissions being set or Tomcat running as `root`.