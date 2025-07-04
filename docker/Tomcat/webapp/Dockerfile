FROM tomcat:10.1.42-jre21
ARG GID=2000
ARG UID=2000

# Secure files (CIS-DI-0008)
RUN chmod -R 751 /usr/bin /usr/sbin

# Create folder and user, and set file permissions
RUN set -eux && \
	# Create default directory for configurations, properties and credential filesystem
	mkdir -p /opt/frank/secrets && \
	mkdir -p /opt/frank/configurations && \
	# Create tomcat user for stepping down from root
	groupadd -g ${GID} tomcat && \
	useradd -u ${UID} -g tomcat -s /usr/sbin/nologin -l tomcat -d ${CATALINA_HOME} && \
	# Change permissions and ownership of files
	chown -hR tomcat:tomcat ${CATALINA_HOME} && \
	chown -hR tomcat:tomcat /opt/frank

# All previous actions are performed as root. Run following instructions and start container as tomcat.
USER tomcat

# Copy environment configuration
COPY --chown=tomcat src/scripts/ /scripts/
RUN cat /scripts/catalinaAdditional.properties >> /usr/local/tomcat/conf/catalina.properties  && \
    rm -f /scripts/catalinaAdditional.properties

# Copy JDBC and JMS drivers
COPY --chown=tomcat target/dependencies/*.jar /opt/frank/drivers/

# Copy webapp war
COPY --chown=tomcat target/dependencies/war/ /usr/local/tomcat/webapps/ROOT/

# Disable JSP re-compilation for Tomcat.
RUN sed -i "s/<\/web-app>//g" /usr/local/tomcat/webapps/ROOT/WEB-INF/web.xml && \
    cat /scripts/web.xml >> /usr/local/tomcat/webapps/ROOT/WEB-INF/web.xml && \
    echo "</web-app>" >> /usr/local/tomcat/webapps/ROOT/WEB-INF/web.xml && \
    rm -f /scripts/web.xml

# Ensure the container is healty
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 CMD curl --fail --silent http://localhost:8080/iaf/api || exit 1

ENTRYPOINT ["/scripts/entrypoint.sh"]
CMD ["catalina.sh", "run"]
