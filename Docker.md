Frank!Framework with Docker
===========================

Docker images are provided, suitable both for local and server use. Images are provided from the registry located at <https://TBD>, the source is avalaible from the [docker-folder](docker/appserver/Tomcat) in this repository.

General use
===========

The container contains the following important directories:
| location | description |
|---|---|
| /opt/frank/resources | For application-wide properties, may contain files or a .jar with all files |
| /opt/frank/configurations | For configurations, may contain a .jar or directories per configuration, when using .jar additional properties per configuration need to be set in resources |
| /opt/frank/testtool | For Larva tests that are included in the image |
| /opt/frank/testtool-ext | For Larva tests that are mounted from the environment |
| /usr/local/tomcat/lib/ | Contains drivers and other dependencies |

The container also contains the following important files:
| location | description |
|---|---|
| /usr/local/tomcat/conf/Catalina/localhost/iaf.xml | mount/copy of your context.xml |
| /usr/local/tomcat/conf/catalina.properties | Server properties, contains default framework values, append if needed |



Considerations
==============

The images are based on Tomcat, all restrictions and considerations that apply to Tomcat also apply to using the provided images.

Special consideration should be taken with secrets. As described on the [Tomcat website](https://cwiki.apache.org/confluence/display/TOMCAT/Password), passwords are stored in plain text. Secrets can be provided via environment variables as `org.apache.tomcat.util.digester.PROPERTY_SOURCE=org.apache.tomcat.util.digester.EnvironmentPropertySource` is set for our images as explained in the [Tomcat documentation](https://tomcat.apache.org/tomcat-8.5-doc/config/systemprops.html#Property_replacements). These secrets are normally visible on a number of pages in the console, to hide those values `properties.hide` should include the environment variables to hide.