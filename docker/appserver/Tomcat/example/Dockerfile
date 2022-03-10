FROM iaf-webapp-tomcat AS iaf-example-tomcat

# copy any additional drivers
COPY target/dependencies/*.jar /usr/local/tomcat/lib/

# copy resources
COPY target/dependencies/resources /opt/frank/resources
#COPY target/dependencies/frank/configuration/*-configurations.jar /opt/frank/configurations/
#COPY target/dependencies/frank/testtool /opt/frank/testtool

# copy context, normally this would be mounted from the environment, but for example a H2 database is used
COPY target/dependencies/customViews /usr/local/tomcat/webapps/ROOT
COPY target/dependencies/context.xml /usr/local/tomcat/conf/Catalina/localhost/ROOT.xml

# Set permissions after copy, to improve startup time
RUN /setPermissions.sh
ENV SET_PERMISSIONS_ON_STARTUP=FALSE