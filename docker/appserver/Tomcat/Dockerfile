ARG TOMCAT_VERSION=8.5.60-jdk8
FROM tomcat:${TOMCAT_VERSION} AS iaf-test-as-tomcat

# copy the frank framework
COPY target/dependencies/ibis-adapterframework-webapp.war /usr/local/tomcat/webapps/iaf.war

# copy JDBC and JMS drivers
COPY target/dependencies/*.jar /usr/local/tomcat/lib/

# copy environment configuration
COPY src/scripts/catalinaAdditional.properties /tmp
RUN cat /tmp/catalinaAdditional.properties >> /usr/local/tomcat/conf/catalina.properties  && rm -f /tmp/catalinaAdditional.properties


# -- instance specific part --

# copy database and jms configuration
COPY src/scripts/context.xml /usr/local/tomcat/conf/Catalina/localhost/iaf.xml

# copy configuration, properties and testscenarios
COPY target/dependencies/frank /opt/frank
