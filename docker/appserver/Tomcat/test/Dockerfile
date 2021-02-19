FROM iaf-as-tomcat AS iaf-test-tomcat


# copy additional environment configuration for iaf-test
COPY src/scripts/catalinaAdditionalTest.properties /tmp
RUN cat /tmp/catalinaAdditionalTest.properties >> /usr/local/tomcat/conf/catalina.properties  && rm -f /tmp/catalinaAdditionalTest.properties

# copy any additional drivers
COPY target/dependencies/*.jar /usr/local/tomcat/lib/

# copy database and jms configuration
COPY src/scripts/context.xml /usr/local/tomcat/conf/Catalina/localhost/iaf.xml

# copy configuration, properties and testscenarios
COPY target/dependencies/frank /opt/frank
