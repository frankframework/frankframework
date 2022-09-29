## Refer to the readme on how to create a base image to use
ARG BASE_IMAGE=oracle/database:21.3.0-xe
FROM ${BASE_IMAGE}

# Copy in setup scripts
COPY *.sql /docker-entrypoint-initdb.d/setup/

## Run with --nowait as is done in extensions to create database to reduce startup time
RUN $ORACLE_BASE/$RUN_FILE --nowait