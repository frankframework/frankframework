ARG TOMCAT_VERSION=8.5.60-jdk8
FROM tomcat:${TOMCAT_VERSION} AS iaf-as-tomcat

# copy the frank framework
COPY target/dependencies/ibis-adapterframework-webapp.war /usr/local/tomcat/webapps/iaf.war

# copy environment configuration
COPY src/scripts/catalinaAdditional.properties /tmp
RUN cat /tmp/catalinaAdditional.properties >> /usr/local/tomcat/conf/catalina.properties  && rm -f /tmp/catalinaAdditional.properties

# copy JDBC and JMS drivers
COPY target/dependencies/*.jar /usr/local/tomcat/lib/
