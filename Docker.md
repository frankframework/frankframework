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
- [Considerations](#Considerations)
  - [HTTPS and security](#HTTPS-and-security)
  - [Secrets](#Secrets)
  - [Non-root](#Non-root)


General use
===========
The image contains an empty framework-instance that needs to be configured before use.

Whether using the container locally or building your own image for use on servers, refer to [Filesystem](#Filesystem) information on which directories and files to mount or copy.

For a list of available tags, see <https://TBD>.

## Local use

To run the image, run the following command, adding environment variables and mounts as needed:

`docker run --publish <hostport>:8080 [-e <name>=<value>] [--mount type=bind,source=<source>,target=<target>] --name <name> TBD/TBD/iaf-as-tomcat[:<tag>]`

## Server use

Please read the [Considerations](#Considerations) before using the image on servers, as the default setup might not be secure enough for your uses.

For use on servers, you need to build your own image that includes the required configuration files. To start building your own image, start your Dockerfile with:

`FROM TBD/TBD/iaf-as-tomcat[:<tag>]`

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
| /usr/local/tomcat/lib | Contains drivers and other dependencies | Contains all Framework required drivers by default |
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

Considerations
==============

The images are based on Tomcat, all restrictions and considerations that apply to Tomcat also apply to using the provided images.

## HTTPS and authentication

By default, the image uses the default server.xml of Tomcat which is not configured for inbound HTTPS traffic and user authentication on the administration console. To secure your application, replace server.xml with a secured version matching your requirements.

Note: The GUI will not load data if accessed via HTTP and `dtap.stage!=LOC`, HTTP-based listeners will still process messages.

## Secrets

Special consideration should be taken with secrets. As described on the [Tomcat website](https://cwiki.apache.org/confluence/display/TOMCAT/Password), passwords are stored in plain text. 

We provide the following options to include secrets:
- Via environment variables as described in the [Tomcat documentation](https://tomcat.apache.org/tomcat-8.5-doc/config/systemprops.html#Property_replacements). By default all environment variables and their values are visible on a number of pages in the console and in the log files, to hide those values `properties.hide` should include the environment variables to hide.
-

## Non-root

This image runs Tomcat as a separate user `tomcat:tomcat` with `UID=1000` and `GID=1000`. To ensure correct file permissions, by default the root user sets the file permissions on startup after which Tomcat is started using `gosu` to step down to `tomcat`. For setups with a large number of files, setting the permissions reduces startup performance, see [Permissions](#Permissions) to set the file permissions during build.