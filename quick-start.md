# Frank!Framework Quick Start with Docker Compose

This guide helps you get up and running quickly with the Frank!Framework using Docker Compose.
The setup includes:

- **Frank!Framework** – the core application
- **Frank!Flow** – a visual configuration tool
- **Swagger UI** – an API documentation viewer
- **Nginx** – a reverse proxy to prevent CORS issues between Swagger UI and the Frank!Framework API

## Contents

<!-- TOC -->
* [Frank!Framework Quick Start with Docker Compose](#frankframework-quick-start-with-docker-compose)
  * [Contents](#contents)
  * [Prerequisites](#prerequisites)
  * [Getting started](#getting-started)
    * [Directory structure](#directory-structure)
    * [Docker Compose file](#docker-compose-file)
    * [Running the setup](#running-the-setup)
    * [What's next](#whats-next)
  * [Mounting files](#mounting-files)
    * [Directories](#directories)
    * [Files](#files)
  * [Environment variables](#environment-variables)
  * [Health and readiness](#health-and-readiness)
  * [Further reading](#further-reading)
<!-- TOC -->

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (includes Docker Compose) or Docker Engine with the [Compose plugin](https://docs.docker.com/compose/install/)
- Docker Compose **v2.22 or later** – required for the `--watch` flag used in this guide

## Getting started

### Directory structure

Create a directory for your project and add a `configurations/` subdirectory where your Frank configurations will live:

```
my-frank-project/
├── compose.yaml
└── configurations/
    └── <YourConfiguration>/
        └── Configuration.xml
```

The `configurations/` directory is synced into the container automatically when using `docker compose up --watch`.

### Docker Compose file

Create a `compose.yaml` file with the following content:

```yaml
# Docker Compose for running the Frank!Framework quickstart setup.
#
# Services:
#   - frankframework: the core application (runs on port 8080)
#   - frank-flow: visual configuration tool (runs on port 8081)
#   - swagger-ui: API docs viewer (runs on port 8082)
#   - nginx: reverse proxy forwarding / and /api/ to prevent CORS issues (runs on port 80)
#
# Start the services with:
#   docker compose up --watch

services:
  frankframework:
    image: frankframework/frankframework:latest
    ports:
      - "8080:8080"
    configs:
      - source: resources.yml
        target: /opt/frank/resources/resources.yml
    environment:
      instance.name: ff-quick-start
      customViews.names: FrankFlow
      customViews.FrankFlow.name: Frank!Flow
      customViews.FrankFlow.url: http://localhost:8081
    develop:
      watch:
        - action: sync
          initial_sync: true
          path: ./configurations/
          target: /opt/frank/configurations/

        # Optional additional watch actions for other directories (e.g., resources, secrets)
        # - action: sync
        #   path: ./testtool
        #   target: /opt/frank/testtool
        # - action: sync
        #   path: ./resources
        #   target: /opt/frank/resources
        # - action: sync+restart
        #   path: ./secrets
        #   target: /opt/frank/secrets

  frank-flow:
    image: frankframework/frank-flow:latest
    ports:
      - "8081:8080"
    volumes:
      - ./configurations/:/opt/frank/configurations
    environment:
      configurations.directory: /opt/frank/configurations

  swagger-ui:
    image: swaggerapi/swagger-ui:v5.32.0
    ports:
      - "8082:8080"
    environment:
      CORS: "false"
      URLS: |
        [
          {"name":"Configurations API","url":"http://localhost:8083/api/webservices/openapi.json"},
          {"name":"Frank!Framework API","url":"http://localhost:8083/api/openapi/v3.yaml"}
        ]

  nginx:
    image: nginx:latest
    ports:
      - "8083:80"
    configs:
      - source: nginx_conf
        target: /etc/nginx/nginx.conf
    depends_on:
      - frankframework
      - swagger-ui

configs:
  # JDBC datasource config for Frank!Framework
  resources.yml:
    content: |
      jdbc:
        - name: "ff-quick-start"
          type: "org.h2.jdbcx.JdbcDataSource"
          url: "jdbc:h2:mem:test;NON_KEYWORDS=VALUE;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=0;"

  # Nginx config used by the nginx service
  nginx_conf:
    content: |
      events {}

      http {
        server {
          listen 80;

          location / {
            proxy_pass http://swagger-ui:8080/;
            proxy_set_header Host $$host;
          }

          location /api/ {
            proxy_pass http://frankframework:8080/iaf/api/;
            proxy_set_header Host $$host;
          }
        }
      }
```

### Running the setup

Start all services with file-watching enabled (so that changes to the `configurations/` directory are synced automatically):

> **Note:** The `--watch` flag requires Docker Compose v2.22 or later. Run `docker compose version` to check. If your version is older, use `docker compose up -d` instead and restart the container manually after making changes.

```shell
docker compose up --watch
```

To stop all services:

```shell
docker compose down
```

### What's next

Once the services are running, the following endpoints will be available:

| URL                       | Description                                                            |
|---------------------------|------------------------------------------------------------------------|
| http://localhost:8080     | **Frank!Framework console** – monitor and manage your Frank application |
| http://localhost:8081     | **Frank!Flow** – visual configuration editor                           |
| http://localhost:8083     | **Swagger UI** (via Nginx proxy) – browse and test the API             |

Open http://localhost:8080 in your browser to access the Frank!Framework console. Frank!Flow is also accessible from the console sidebar under the "Frank!Flow" menu item.

Use http://localhost:8083 to explore the API through Swagger UI. The Nginx proxy at this address forwards API requests to the Frank!Framework, avoiding CORS issues.

From here you can:
- Place your Frank configuration files in the `configurations/` directory and they will be synced into the running container automatically.
- Explore the Frank!Framework console to monitor adapters, view logs, and test your configurations.
- Use Frank!Flow to visually create and edit configurations.
- Use the Swagger UI to explore and test the Frank!Framework REST API.

## Mounting files

The Frank!Framework container uses several well-known directories and files. You can mount these into the container to provide your own content.

### Directories

| Directory                   | Description                                                                                                                                                                                                                                                                                                                                                                                   |
|-----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `/opt/frank/configurations` | For configurations; may contain a directory with files per configuration or a JAR containing a directory per configuration. When `Configuration.xml` is not located at `<configurationName>/Configuration.xml`, your resources should include a property `configurations.<configurationName>.configurationFile` containing the path to the Configuration.xml. Configurations found are loaded automatically. Can be disabled by setting `configurations.directory.autoLoad=false`. |
| `/opt/frank/resources`      | For application-wide properties; may contain files or a JAR with all files. Minimum required properties are `instance.name` and `configurations.names`, which can also be set using environment variables.                                                                                                                                                                                    |
| `/opt/frank/testtool`       | For Larva tests included in the image.                                                                                                                                                                                                                                                                                                                                                        |
| `/opt/frank/testtool-ext`   | For Larva tests mounted from the environment.                                                                                                                                                                                                                                                                                                                                                 |
| `/opt/frank/secrets`        | Credential storage (`credentials.properties` will be read by default). See [Secrets](Docker.md#secrets).                                                                                                                                                                                                                                                                                     |
| `/opt/frank/drivers`        | Contains driver JARs. See [Drivers](Docker.md#drivers).                                                                                                                                                                                                                                                                                                                                       |
| `/opt/frank/plugins`        | Contains plugin JARs.                                                                                                                                                                                                                                                                                                                                                                         |
| `/usr/local/tomcat/logs`    | Log directory.                                                                                                                                                                                                                                                                                                                                                                                |

To mount additional directories, add volume or watch entries to the `frankframework` service in `compose.yaml`. For example, to mount a `resources/` directory:

```yaml
services:
  frankframework:
    ...
    volumes:
      - ./resources/:/opt/frank/resources
```

Or, using the file-watch approach (recommended for local development):

```yaml
develop:
  watch:
    - action: sync
      path: ./resources
      target: /opt/frank/resources
```

> **Note:** The image runs Tomcat as a separate user `tomcat:tomcat` with `UID=2000` and `GID=2000`. Keep this in mind when copying or mounting files to the container – the files need to be owned by `tomcat:tomcat` instead of `root`.

### Files

| File                                       | Description                                                                                                                                               |
|--------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `/opt/frank/resources/resources.yml`       | Mount or copy of your `resources.yml`. Use hostname `host.docker.internal` to reach the host machine for local testing. Changes require a container restart (when using volumes, use the `sync+restart` watch action instead). |
| `/usr/local/tomcat/conf/server.xml`        | Mount or copy of your `server.xml`. Contains the default Tomcat server configuration; replace to secure your application.                                 |
| `/usr/local/tomcat/conf/catalina.properties` | Server properties containing default framework values. Do not replace this file; use [environment variables](#environment-variables) or append to it instead. |

## Environment variables

Environment variables can be used to set properties. They have the highest precedence and override application properties set in `.properties` files supplied by Tomcat, resources, and configurations.

Set environment variables in `compose.yaml` under the `environment` key:

```yaml
services:
  frankframework:
    ...
    environment:
      instance.name: my-frank-app
      dtap.stage: LOC
      configurations.directory.autoLoad: "false"
```

> **Important:** Do not use environment variables for secrets. See [Secrets](Docker.md#secrets) for more information.

## Health and readiness

The health and readiness of the container can be monitored by polling the `/iaf/api/server/health` endpoint:

- **HTTP 200** – configurations are loaded and running
- **HTTP 503** – one or more configurations are in a non-running state

To check the health of a specific adapter:

```
GET /iaf/api/configurations/{configuration}/adapters/{name}/health
```

You can add a `healthcheck` to the service in `compose.yaml`:

```yaml
services:
  frankframework:
    ...
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/iaf/api/server/health"]
      interval: 10s
      timeout: 5s
      retries: 5
```

## Further reading

- [Frank!Framework with Docker](Docker.md) – comprehensive documentation on Docker image usage, filesystem, logging, environment variables, secrets, drivers, and more
- [Frank!Manual](https://frank-manual.readthedocs.io) – full documentation for the Frank!Framework
- [DockerHub – frankframework/frankframework](https://hub.docker.com/r/frankframework/frankframework) – available image tags
- [Helm charts](https://github.com/frankframework/charts) – for Kubernetes deployments
