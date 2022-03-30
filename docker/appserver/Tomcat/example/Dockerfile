FROM iaf-webapp-tomcat AS iaf-example-tomcat

# copy any additional drivers
COPY target/dependencies/*.jar /usr/local/tomcat/lib/

# copy resources
COPY target/dependencies/resources /opt/frank/resources

# copy context, normally this file is mounted from the environment
# Example uses an H2 internal database as it does not need to store actual data, so the context.xml can be part of the image
COPY target/dependencies/context.xml /usr/local/tomcat/conf/Catalina/localhost/ROOT.xml

# Set permissions after copy, to improve startup time
RUN /setPermissions.sh
ENV SET_PERMISSIONS_ON_STARTUP=FALSE