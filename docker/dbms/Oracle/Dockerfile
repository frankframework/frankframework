## Refer to the readme on how to create a base image to use
ARG BASE_IMAGE=oracle/database:21.3.0-xe
FROM ${BASE_IMAGE}

# Copy in setup scripts
COPY *.sql /docker-entrypoint-initdb.d/setup/

# Set SGA and PGA to prevent OA-00821, see https://github.com/oracle/docker-images/issues/2216
RUN sed -i "s/1536/1600/" /opt/oracle/product/21c/dbhomeXE/assistants/dbca/templates/XE_Database.dbc && \
	sed -i "s/512/256/" /opt/oracle/product/21c/dbhomeXE/assistants/dbca/templates/XE_Database.dbc && \
	# Run with --nowait as is done in extensions to create database to reduce startup time
	$ORACLE_BASE/$RUN_FILE --nowait