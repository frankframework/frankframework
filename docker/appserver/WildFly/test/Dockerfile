FROM iaf-as-wildfly AS iaf-example-wildfly

# copy any additional drivers
COPY target/dependencies/*.jar /opt/jboss/wildfly/standalone/lib/

# copy database and jms configuration
COPY src/scripts/standalone.xml /opt/jboss/wildfly/standalone/configuration/standalone.xml

# copy configuration, properties and testscenarios
COPY target/dependencies/frank/configuration/*-resources.jar /opt/jboss/wildfly/standalone/lib/ext/
COPY target/dependencies/frank/configuration/*-configurations.jar /opt/jboss/wildfly/standalone/lib/ext/
COPY target/dependencies/frank/testtool /opt/frank/testtool


COPY src/scripts/add_modules.py /home/add_modules.py

RUN python /home/add_modules.py