FROM iaf-as-tomcat AS iaf-webapp-tomcat

# copy any additional drivers
COPY target/dependencies/*.jar /usr/local/tomcat/lib/

# copy war
COPY target/dependencies/war /usr/local/tomcat/webapps/ROOT