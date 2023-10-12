FROM scalified/jboss-eap:7.4.0 AS jboss-eap-with-python


# Create credential store in separate image
FROM jboss-eap-with-python AS jboss-iaf-test-CredentialStoreCreator
COPY src/configuration/createCS.sh /home/createCS.sh
COPY target/dependencies/secrets /opt/frank/secrets

RUN /home/createCS.sh

FROM jboss-eap-with-python
# Copy dependencies
COPY target/dependencies/*.jar /opt/jboss/standalone/lib/ext/
COPY target/dependencies/*.rar /opt/jboss/standalone/deployments/

# Copy in standalone-configuration.xml
COPY src/configuration/standalone.xml /opt/jboss/standalone/configuration/standalone.xml

# Copy in the created credential store
# chown is needed because otherwise permissions of the directory conflict with JBoss, resulting in WFLYSRV0126
COPY --from=jboss-iaf-test-CredentialStoreCreator --chown=jboss:jboss /opt/jboss/standalone/data/CS.jceks /opt/jboss/standalone/data/CS.jceks

# Copy configuration script for modules
COPY src/configuration/configuration.py /home/configuration.py
RUN python /home/configuration.py

# Add test scenarios explictly for easy CI
COPY target/dependencies/frank /opt/frank

# Copy war
COPY target/dependencies/ibis-adapterframework-test.war /opt/jboss/standalone/deployments/iaf-test.war

USER root
RUN useradd -u 1001 jboss
RUN chown jboss:jboss -R /opt/jboss/standalone/
USER jboss

EXPOSE 8080
EXPOSE 9990
