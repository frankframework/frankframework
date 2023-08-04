ARG TOMCAT_VERSION=8.5.81-jdk8
FROM tomcat:${TOMCAT_VERSION} AS iaf-as-tomcat
ARG TOMCAT_GROUPID=1000
ARG TOMCAT_USERID=1000

RUN set -eux && \
	# Create default directory for configurations, properties and credential filesystem
	mkdir -p /opt/frank/secrets && \
	# Create tomcat user for stepping down from root
	groupadd -g ${TOMCAT_GROUPID} tomcat && \
	useradd -u ${TOMCAT_USERID} -g tomcat -s /usr/sbin/nologin tomcat && \
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

