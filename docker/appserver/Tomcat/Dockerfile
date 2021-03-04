ARG TOMCAT_VERSION=8.5.60-jdk8
FROM tomcat:${TOMCAT_VERSION} AS iaf-as-tomcat
ARG TOMCAT_GROUPID=1000
ARG TOMCAT_USERID=1000

# By default, entrypoint.sh will set the permissions on startup
# For large numbers of folders and files this will slow down startup, so allow disabling setting permissions on startup
# This requires the setPermissions.sh to be executed as last step of the build process to make sure the permissions are set correctly
ENV SET_PERMISSIONS_ON_STARTUP=TRUE

# Install gosu for stepping down from root in entrypoint after setting permissions
RUN set -eux; \
	apt-get update; \
	apt-get install -y gosu; \
	rm -rf /var/lib/apt/lists/*; \
	gosu nobody true; \
	# Add tomcat user
	groupadd -r tomcat -g ${TOMCAT_GROUPID}; \
	useradd -u ${TOMCAT_USERID} -g tomcat -d ${CATALINA_HOME} -s /sbin/nologin tomcat; \
	# create default directory for configurations and properties
	mkdir -p /opt/frank

# Copy entrypoint script
COPY src/scripts/*.sh /

# copy the frank framework
COPY target/dependencies/ibis-adapterframework-webapp.war /usr/local/tomcat/webapps/iaf.war

# copy environment configuration
COPY src/scripts/catalinaAdditional.properties /tmp
RUN cat /tmp/catalinaAdditional.properties >> /usr/local/tomcat/conf/catalina.properties  && rm -f /tmp/catalinaAdditional.properties

# copy JDBC and JMS drivers
COPY target/dependencies/*.jar /usr/local/tomcat/lib/

ENTRYPOINT ["/entrypoint.sh"]
CMD ["catalina.sh", "run"]