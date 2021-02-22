ARG IAF_VERSION=7.6-SNAPSHOT
FROM iaf-as-tomcat:${IAF_VERSION} AS iaf-example-tomcat

# copy any additional drivers
COPY target/dependencies/*.jar /usr/local/tomcat/lib/

# copy database and jms configuration
COPY src/scripts/context.xml /usr/local/tomcat/conf/Catalina/localhost/iaf.xml

# copy configuration, properties and testscenarios
COPY target/dependencies/frank/configuration/*-configurations.jar /opt/frank/configurations/
COPY target/dependencies/frank/configuration/*-resources.jar /opt/frank/resources/
COPY target/dependencies/frank/testtool /opt/frank/testtool
