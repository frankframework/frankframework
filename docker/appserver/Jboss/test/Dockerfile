FROM iaf-as-jboss AS iaf-test-jboss

# copy any additional drivers
COPY target/dependencies/*.jar /home/jboss/jboss-eap-6.4/standalone/lib/

# copy database and jms configuration
COPY src/scripts/standalone.xml /home/jboss/jboss-eap-6.4/standalone/configuration/standalone.xml

# copy configuration, properties and testscenarios
COPY target/dependencies/frank/configuration/*-resources.jar /home/jboss/jboss-eap-6.4/standalone/lib/ext/
COPY target/dependencies/frank/configuration/*-configurations.jar /home/jboss/jboss-eap-6.4/standalone/lib/ext/
COPY target/dependencies/frank/testtool /opt/frank/testtool


COPY src/scripts/add_modules.py /home/add_modules.py


USER root
RUN yum -y install python3

USER jboss
RUN python3 /home/add_modules.py