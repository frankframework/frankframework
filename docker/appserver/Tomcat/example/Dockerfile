FROM iaf-as-tomcat AS iaf-example-tomcat

# copy any additional drivers
COPY target/dependencies/*.jar /usr/local/tomcat/lib/

# copy database and jms configuration
COPY target/dependencies/frank/context/context.xml /usr/local/tomcat/conf/Catalina/localhost/iaf.xml

# copy configuration, properties and testscenarios
COPY target/dependencies/frank/configuration/*-resources.jar /opt/frank/resources/
COPY target/dependencies/frank/configuration/*-configurations.jar /opt/frank/configurations/
COPY target/dependencies/frank/testtool /opt/frank/testtool

# Set permissions after copy, to improve startup time
RUN /setPermissions.sh
ENV SET_PERMISSIONS_ON_STARTUP=FALSE