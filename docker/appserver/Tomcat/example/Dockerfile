FROM iaf-as-tomcat AS iaf-example-tomcat

# copy any additional drivers
COPY target/dependencies/*.jar /usr/local/tomcat/lib/

# copy war
COPY target/dependencies/ibis-adapterframework-example.war /usr/local/tomcat/webapps/ROOT.war

# copy configuration, properties and testscenarios
#COPY target/dependencies/frank/configuration/*-resources.jar /opt/frank/resources/
#COPY target/dependencies/frank/configuration/*-configurations.jar /opt/frank/configurations/
#COPY target/dependencies/frank/testtool /opt/frank/testtool

# Set permissions after copy, to improve startup time
RUN /setPermissions.sh
ENV SET_PERMISSIONS_ON_STARTUP=FALSE