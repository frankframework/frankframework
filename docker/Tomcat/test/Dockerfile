ARG VERSION=latest
FROM nexus.frankframework.org/frank-framework:${VERSION}

# Copy additional environment configuration for iaf-test
COPY --chown=tomcat src/scripts/catalinaAdditionalTest.properties /tmp
RUN cat /tmp/catalinaAdditionalTest.properties >> /usr/local/tomcat/conf/catalina.properties  && rm -f /tmp/catalinaAdditionalTest.properties

# Copy configurations
COPY --chown=tomcat target/resources /opt/frank/resources

# Copy context.xml
COPY --chown=tomcat target/context.xml /usr/local/tomcat/conf/Catalina/localhost/ROOT.xml

# Credentials need to be mounted in /opt/frank/secrets at runtime
