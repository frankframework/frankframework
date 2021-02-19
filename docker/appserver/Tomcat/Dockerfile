ARG ALPINE_VERSION=latest
ARG TOMCAT_VERSION=8.5.60-jdk8

# unpack the framework in a separate image to prevent an additional layer
# adding the framework already unpacked allows adding properties from the resources directory
FROM alpine:${ALPINE_VERSION} AS unpack
RUN apk add -U unzip
COPY target/dependencies/ibis-adapterframework-webapp.war /tmp/iaf.war
RUN unzip -qq /tmp/iaf.war -d /tmp/iaf

FROM tomcat:${TOMCAT_VERSION} AS iaf-as-tomcat
# copy the frank framework
COPY --from=unpack /tmp/iaf /usr/local/tomcat/webapps/iaf

# copy environment configuration
COPY src/scripts/catalinaAdditional.properties /tmp
RUN cat /tmp/catalinaAdditional.properties >> /usr/local/tomcat/conf/catalina.properties  && rm -f /tmp/catalinaAdditional.properties

# copy JDBC and JMS drivers
COPY target/dependencies/*.jar /usr/local/tomcat/lib/
