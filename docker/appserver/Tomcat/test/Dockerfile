ARG IAF_VERSION=7.6-SNAPSHOT
FROM iaf-as-tomcat:${IAF_VERSION} AS iaf-test-tomcat

# copy any additional drivers
COPY target/dependencies/*.jar /usr/local/tomcat/lib/

# copy database and jms configuration
COPY src/scripts/context.xml /usr/local/tomcat/conf/Catalina/localhost/iaf.xml

# copy configuration, properties and testscenarios
COPY target/dependencies/frank /opt/frank
