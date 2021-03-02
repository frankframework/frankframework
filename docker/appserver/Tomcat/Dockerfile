ARG TOMCAT_VERSION=8.5.60-jdk8
FROM tomcat:${TOMCAT_VERSION} AS iaf-as-tomcat
ARG TOMCAT_GROUPID=1000
ARG TOMCAT_USERID=1000

# copy the frank framework
COPY target/dependencies/ibis-adapterframework-webapp.war /usr/local/tomcat/webapps/iaf.war

# copy environment configuration
COPY src/scripts/catalinaAdditional.properties /tmp
RUN cat /tmp/catalinaAdditional.properties >> /usr/local/tomcat/conf/catalina.properties  && rm -f /tmp/catalinaAdditional.properties

# copy JDBC and JMS drivers
COPY target/dependencies/*.jar /usr/local/tomcat/lib/

# Change user permissions
RUN echo ${CATALINA_HOME} && \
	groupadd -r tomcat -g ${TOMCAT_GROUPID} && \
	useradd -u ${TOMCAT_USERID} -g tomcat -d ${CATALINA_HOME} -s /sbin/nologin tomcat && \
	chown -R tomcat:tomcat ${CATALINA_HOME} && \
	chmod -R 400 ${CATALINA_HOME}/conf/* && \
	mkdir -p /opt/frank && \
	chown -R tomcat:tomcat /opt/frank && \
	chmod -R 700 /opt/frank
USER tomcat