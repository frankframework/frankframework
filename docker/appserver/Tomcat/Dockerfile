ARG TOMCAT_VERSION=8.5.60-jdk8
FROM tomcat:${TOMCAT_VERSION} AS iaf-test-as-tomcat
COPY target/dependencies/*.jar /usr/local/tomcat/lib/
COPY target/dependencies/ibis-adapterframework-webapp.war /usr/local/tomcat/webapps/iaf.war
COPY src/scripts/context.xml /usr/local/tomcat/conf/Catalina/localhost/iaf.xml
COPY target/dependencies/frank /opt/frank
COPY src/scripts/catalinaAdditional.properties /tmp
RUN cat /tmp/catalinaAdditional.properties >> /usr/local/tomcat/conf/catalina.properties  && rm -f /tmp/catalinaAdditional.properties
