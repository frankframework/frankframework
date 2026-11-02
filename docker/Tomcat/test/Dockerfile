ARG FF_VERSION=latest
# Make repository variable so iaf-test can use a private image
ARG REGISTRY=nexus.frankframework.org
FROM ${REGISTRY}/frankframework:${FF_VERSION}

# Copy additional environment configuration for iaf-test
COPY --chown=tomcat src/scripts/catalinaAdditionalTest.properties /tmp/
RUN cat /tmp/catalinaAdditionalTest.properties >> /usr/local/tomcat/conf/catalina.properties  && \
     rm -f /tmp/catalinaAdditionalTest.properties

# Copy secrets
COPY --chown=root:root --chmod=a=rX target/secrets/ /opt/frank/secrets/

# Copy configurations
COPY --chown=root:root --chmod=a=rX target/resources/ /opt/frank/resources/

# Copy scenarios
COPY --chown=root:root --chmod=a=rX target/scenarios/ /opt/frank/testtool/

# Copy context.xml
COPY --chown=root:root --chmod=a=r target/context.xml /usr/local/tomcat/conf/Catalina/localhost/iaf-test.xml

# Copy database and JMS drivers
COPY --chown=root:root --chmod=a=r target/dependencies/*.jar /opt/frank/drivers/

# Change context path to iaf-test
# Because of permissions, we have to temporarily do this as user root
USER root
RUN mv /usr/local/tomcat/webapps/ROOT /usr/local/tomcat/webapps/iaf-test

# Make sure we run as tomcat user, not root
USER tomcat

# Ensure the container is healty
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 CMD curl --fail --silent http://localhost:8080/iaf-test/iaf/api/server/health || exit 1

# Note: Credentials need to be mounted in /opt/frank/secrets at runtime
