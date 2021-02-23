Frank!Framework with Docker
===========================

Docker images are provided, suitable both for local and server use. Images are provided from the registry located at <https://TBD>, the source is avalaible from the [docker-folder](docker/appserver/Tomcat) in this repository.

General use
===========
The image contains an empty framework-instance that needs to be configured before use.

The image contains the following directories to be configured:
| directory | description | notes |
|---|---|---|
| /opt/frank/resources | For application-wide properties, may contain files or a .jar with all files | Minimum required properties to set are `instance.name` and `configurations.names`, can also be set using environment variables |
| /opt/frank/configurations | For configurations, may contain a directory with files per configuration or a .jar containing a directory per configuration | When Configuration.xml is not located at <configurationName>/Configuration.xml, your resources should include a property `configurations.<configurationName>.configurationFile` containing the path to the Configuration.xml |
| /opt/frank/testtool | For Larva tests that are included in the image | |
| /opt/frank/testtool-ext | For Larva tests that are mounted from the environment | |
| /usr/local/tomcat/lib | Contains drivers and other dependencies | Contains all Framework required drivers by default |

The image also contains the following files to be configured:
| file | description | notes |
|---|---|---|
| /usr/local/tomcat/conf/Catalina/localhost/iaf.xml | mount/copy of your context.xml | Use hostname `host.docker.internal` to get to the host machine for local testing. Changing this file will require a new instance to be started, it cannot be reloaded |
| /usr/local/tomcat/conf/catalina.properties | Server properties, contains default framework values | Do not replace this file, use environment variables or append to the file, see [Dockerfile](docker/appserver/Tomcat/Dockerfile) for an example |

To run the image, run the following command, adding environment variables and mounts as needed:

`docker run --publish <hostport>:8080 [-e <name>=<value>] [--mount type=bind,source=<source>,target=<target>] --name <name> TBD/TBD/iaf-as-tomcat[:<version>]`

To start building your own image based on the provided image, start your Dockerfile with:

`FROM TBD/TBD/iaf-as-tomcat<:version>`

Considerations
==============

The images are based on Tomcat, all restrictions and considerations that apply to Tomcat also apply to using the provided images.

Special consideration should be taken with secrets. As described on the [Tomcat website](https://cwiki.apache.org/confluence/display/TOMCAT/Password), passwords are stored in plain text. Secrets can be provided via environment variables as `org.apache.tomcat.util.digester.PROPERTY_SOURCE=org.apache.tomcat.util.digester.EnvironmentPropertySource` is set for our images as explained in the [Tomcat documentation](https://tomcat.apache.org/tomcat-8.5-doc/config/systemprops.html#Property_replacements). These secrets are normally visible on a number of pages in the console, to hide those values `properties.hide` should include the environment variables to hide.