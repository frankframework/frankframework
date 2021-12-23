ARG IMAGE_VERSION
ARG DOCKER_REPO
FROM ${DOCKER_REPO}:${IMAGE_VERSION}
ARG SERVER_TYPE

# copy JDBC and JMS drivers
COPY target/dependencies/*.jar ${JBOSS_HOME}/standalone/deployments/

COPY src/scripts/add_global_modules.py /home/add_global_modules.py

USER root
RUN if [ "$SERVER_TYPE" = "jboss" ]; then yum -y --nogpgcheck install python3; fi

USER jboss
RUN if [ "$SERVER_TYPE" = "jboss" ]; then python3 /home/add_global_modules.py; else python /home/add_global_modules.py; fi