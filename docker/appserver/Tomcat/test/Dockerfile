FROM iaf-as-tomcat AS iaf-test-tomcat


# copy additional environment configuration for iaf-test
COPY src/scripts/catalinaAdditionalTest.properties /tmp
RUN cat /tmp/catalinaAdditionalTest.properties >> /usr/local/tomcat/conf/catalina.properties  && rm -f /tmp/catalinaAdditionalTest.properties

# copy war
COPY target/dependencies/ibis-adapterframework-test.war /usr/local/tomcat/webapps/ROOT.war

# copy any additional drivers
COPY target/dependencies/*.jar /usr/local/tomcat/lib/

# provide database credentials
RUN mkdir -p /opt/frank/secrets/testiaf_user \
	&& echo "testiaf_user"   > /opt/frank/secrets/testiaf_user/username \
	&& echo "testiaf_user00" > /opt/frank/secrets/testiaf_user/password

# copy configuration, properties and testscenarios
#COPY target/dependencies/frank/configuration/*-resources.jar /opt/frank/resources/
#COPY target/dependencies/frank/configuration/*-configurations.jar /opt/frank/configurations/
#COPY target/dependencies/frank/testtool /opt/frank/testtool

# Set permissions after copy, to improve startup time
RUN /setPermissions.sh
ENV SET_PERMISSIONS_ON_STARTUP=FALSE