FROM iaf-webapp-tomcat AS iaf-example-tomcat

# copy any additional drivers
COPY --chown=tomcat target/dependencies/*.jar /usr/local/tomcat/lib/

# copy resources
COPY --chown=tomcat target/dependencies/resources /opt/frank/resources

# copy context, normally this file is mounted from the environment
# The example webapp uses an H2 internal database as it does not need to store actual data, so the context.xml can be part of the image
COPY --chown=tomcat target/dependencies/context.xml /usr/local/tomcat/conf/Catalina/localhost/ROOT.xml
