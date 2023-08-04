FROM iaf-as-tomcat AS iaf-webapp-tomcat

# copy any additional drivers
COPY --chown=tomcat target/dependencies/*.jar /usr/local/tomcat/lib/

# copy war
COPY --chown=tomcat target/dependencies/war /usr/local/tomcat/webapps/ROOT
