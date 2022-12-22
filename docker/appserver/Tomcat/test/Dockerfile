FROM iaf-as-tomcat AS iaf-test-tomcat


# copy additional environment configuration for iaf-test
COPY --chown=tomcat src/scripts/catalinaAdditionalTest.properties /tmp
RUN cat /tmp/catalinaAdditionalTest.properties >> /usr/local/tomcat/conf/catalina.properties  && rm -f /tmp/catalinaAdditionalTest.properties

# copy war
COPY --chown=tomcat target/dependencies/ibis-adapterframework-test.war /usr/local/tomcat/webapps/iaf-test.war

# copy any additional drivers
COPY --chown=tomcat target/dependencies/*.jar /usr/local/tomcat/lib/

# Credentials need to be mounted in /opt/frank/secrets at runtime