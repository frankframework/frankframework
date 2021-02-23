Frank!Framework with Docker
===========================

Docker images are provided, suitable both for local and server use. Images are provided from the registry located at <https://TBD>, the source is avalaible from the [docker-folder](docker/appserver/Tomcat) in this repository.

General use
===========

The container contains the following important directories:
| directory | description | notes |
|---|---|---|
| /opt/frank/resources | For application-wide properties, may contain files or a .jar with all files |
| /opt/frank/configurations | For configurations, may contain a .jar or directory per configuration or a .jar containing directories per configuration | When using a .jar with multiple configurations, your resources should include a property configurations.<configurationName>.configurationFile containing the path to the Configuration.xml in the .jar |
| /opt/frank/testtool | For Larva tests that are included in the image | |
| /opt/frank/testtool-ext | For Larva tests that are mounted from the environment | |
| /usr/local/tomcat/lib | Contains drivers and other dependencies | |

The container also contains the following important files:
| file | description | notes |
|---|---|---|
| /usr/local/tomcat/conf/Catalina/localhost/<web.contextpath>.xml | mount/copy of your context.xml | web.contextpath=ROOT if not set in catalina.properties |
| /usr/local/tomcat/conf/catalina.properties | Server properties, contains default framework values | Do not replace this file, but append to it if necessary, see the [Dockerfile](docker/appserver/Tomcat/Dockerfile) for an example |



Considerations
==============

The images are based on Tomcat, all restrictions and considerations that apply to Tomcat also apply to using the provided images.

Special consideration should be taken with secrets. As described on the [Tomcat website](https://cwiki.apache.org/confluence/display/TOMCAT/Password), passwords are stored in plain text. Secrets can be provided via environment variables as `org.apache.tomcat.util.digester.PROPERTY_SOURCE=org.apache.tomcat.util.digester.EnvironmentPropertySource` is set for our images as explained in the [Tomcat documentation](https://tomcat.apache.org/tomcat-8.5-doc/config/systemprops.html#Property_replacements). These secrets are normally visible on a number of pages in the console, to hide those values `properties.hide` should include the environment variables to hide.