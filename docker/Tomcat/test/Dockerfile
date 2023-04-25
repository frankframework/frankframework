ARG FF_VERSION=latest
# Make repository variable so iaf-test can use a private image
ARG REGISTRY=nexus.frankframework.org
FROM ${REGISTRY}/frank-framework:${FF_VERSION}

# Copy additional environment configuration for iaf-test
COPY --chown=tomcat src/scripts/catalinaAdditionalTest.properties /tmp
RUN cat /tmp/catalinaAdditionalTest.properties >> /usr/local/tomcat/conf/catalina.properties  && rm -f /tmp/catalinaAdditionalTest.properties

# Copy secrets
COPY --chown=tomcat target/secrets /opt/frank/secrets

# Copy configurations
COPY --chown=tomcat target/resources /opt/frank/resources

# Copy scenarios
COPY --chown=tomcat target/scenarios /opt/frank/testtool

# Copy context.xml
COPY --chown=tomcat target/context.xml /usr/local/tomcat/conf/Catalina/localhost/iaf-test.xml

# Change context path to iaf-test
RUN mv /usr/local/tomcat/webapps/ROOT /usr/local/tomcat/webapps/iaf-test

# Credentials need to be mounted in /opt/frank/secrets at runtime
