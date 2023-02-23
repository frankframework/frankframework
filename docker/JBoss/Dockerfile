ARG JBOSS_VERSION=7.3.0-centos
FROM daggerok/jboss-eap-7.3:${JBOSS_VERSION} AS jboss-eap-with-python

USER root
RUN sed -i 's/mirrorlist/#mirrorlist/g' /etc/yum.repos.d/CentOS-* \
	&& sed -i 's|#baseurl=http://mirror.centos.org|baseurl=http://vault.centos.org|g' /etc/yum.repos.d/CentOS-* \
	&& yum install -y --nogpgcheck python3
RUN mkdir /home/jboss/jboss-eap-7.3/standalone/data
RUN chown jboss:jboss /home/jboss/jboss-eap-7.3/standalone/data
USER jboss

# Create credential store in separate image
FROM jboss-eap-with-python AS jboss-iaf-test-CredentialStoreCreator
COPY src/configuration/createCS.sh /home/createCS.sh
COPY target/dependencies/secrets /opt/frank/secrets

RUN /home/createCS.sh

FROM jboss-eap-with-python
# Copy dependencies
COPY target/dependencies/*.jar /home/jboss/jboss-eap-7.3/standalone/lib/ext/
COPY target/dependencies/*.rar /home/jboss/jboss-eap-7.3/standalone/deployments/

# Copy in standalone-configuration.xml
COPY src/configuration/standalone.xml /home/jboss/jboss-eap-7.3/standalone/configuration/standalone.xml

# Copy in the created credential store
# chown is needed because otherwise permissions of the directory conflict with JBoss, resulting in WFLYSRV0126
COPY --from=jboss-iaf-test-CredentialStoreCreator --chown=jboss:jboss /home/jboss/jboss-eap-7.3/standalone/data/CS.jceks /home/jboss/jboss-eap-7.3/standalone/data/CS.jceks

# Copy configuration script for modules
COPY src/configuration/configuration.py /home/configuration.py
RUN python3 /home/configuration.py

# Add test scenarios explictly for easy CI
COPY target/dependencies/frank /opt/frank

# Copy war
COPY target/dependencies/ibis-adapterframework-test.war /home/jboss/jboss-eap-7.3/standalone/deployments/iaf-test.war
