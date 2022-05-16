ARG TOMCAT_VERSION=8.5.76-jdk8
FROM tomcat:${TOMCAT_VERSION} AS iaf-as-tomcat
ARG TOMCAT_GROUPID=1000
ARG TOMCAT_USERID=1000

# By default, entrypoint.sh will set the permissions on startup
# For large numbers of folders and files this will slow down startup, so allow disabling setting permissions on startup
# This requires the setPermissions.sh to be executed as last step of the build process to make sure the permissions are set correctly
ENV SET_PERMISSIONS_ON_STARTUP=TRUE

# Install gosu for stepping down from root in entrypoint after setting permissions
RUN set -eux \
	&& apt-get update \
	&& apt-get install -y --no-install-recommends gosu \
	&& rm -rf /var/lib/apt/lists/* \
	&& gosu nobody true \
	# Add tomcat user
	&& groupadd -r tomcat -g ${TOMCAT_GROUPID} \
	&& useradd -u ${TOMCAT_USERID} -g tomcat -d ${CATALINA_HOME} -s /usr/sbin/nologin tomcat \
	# create default directory for configurations and properties
	&& mkdir -p /opt/frank \
	# prepare root/mountpoint for credential filesystem
	&& mkdir /opt/frank/secrets

# Copy entrypoint script
COPY src/scripts/*.sh /
RUN chmod +rx /*.sh

# copy environment configuration
COPY src/scripts/catalinaAdditional.properties /tmp
RUN cat /tmp/catalinaAdditional.properties >> /usr/local/tomcat/conf/catalina.properties  && rm -f /tmp/catalinaAdditional.properties

# copy JDBC and JMS drivers
COPY target/dependencies/*.jar /usr/local/tomcat/lib/

# entrypoint.sh is used to set file permissions and change user to "tomcat"
ENTRYPOINT ["/entrypoint.sh"]
CMD ["catalina.sh", "run"]
