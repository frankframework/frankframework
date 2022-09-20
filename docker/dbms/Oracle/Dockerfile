ARG BASE_IMAGE=oracle/database:21.3.0-xe
FROM ${BASE_IMAGE}

# Copy in setup scripts
COPY *.sql /docker-entrypoint-initdb.d/setup