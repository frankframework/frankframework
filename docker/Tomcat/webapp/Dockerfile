FROM tomcat:8.5.93-jre11
ARG GID=1000
ARG UID=1000

# Secure files (CIS-DI-0008)
RUN chmod -R 751 /usr/bin /usr/sbin

# Create folder and user, and set file permissions
RUN set -eux && \
	# Create default directory for configurations, properties and credential filesystem
	mkdir -p /opt/frank/secrets && \
	# Create tomcat user for stepping down from root
	groupadd -g ${GID} tomcat && \
	useradd -u ${UID} -g tomcat -s /usr/sbin/nologin -l tomcat && \
	# Change permissions and ownership of files
	chown -hR tomcat:tomcat ${CATALINA_HOME} && \
	chown -hR tomcat:tomcat /opt/frank

# All previous actions are performed as root. Run following instructions and start container as tomcat.
USER tomcat

# Copy environment configuration
COPY --chown=tomcat src/scripts/catalinaAdditional.properties /tmp
RUN cat /tmp/catalinaAdditional.properties >> /usr/local/tomcat/conf/catalina.properties  && rm -f /tmp/catalinaAdditional.properties

# Copy JDBC and JMS drivers
COPY --chown=tomcat target/dependencies/*.jar /usr/local/tomcat/lib/

# Copy webapp war
COPY --chown=tomcat target/dependencies/war /usr/local/tomcat/webapps/ROOT

# Ensure the container is healty
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 CMD curl --fail --silent http://localhost:8080/iaf/api || exit 1
