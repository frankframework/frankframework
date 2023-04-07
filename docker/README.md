# Frank!Framework - Docker

This module contains Docker images for various appservers to test the Frank!Framework with. 
The Docker images have been customized to run without needing much configuration. 
The images contain the test configurations and secrets to connect with our [auxiliary images](https://github.com/ibissource/iaf-ci-images) which include the databases and MQs.

This folder also contains a docker compose file to make it easy to run.  

## Building test image

Use Maven to build the project and the Docker image

```shell
mvn install -P docker,Tomcat
```

## Running test image

First make sure that you can access the auxiliary images. This can be done by building them yourself, or by using the prebuild images in the private Docker registry `private.docker.nexus.frankframework.org`. The private repository requires [login](https://docs.docker.com/engine/reference/commandline/login/).

Use Docker compose to run any combination of test image, database and MQ.
The properties will be resolved automatically.
Combine the YAML files in this folder to start your test environment. 

For example, if you want to run WildFly with MariaDB and Artemis, run this command:

```shell
docker compose -f wildfly.yml -f mariadb.yml -f artemis.yml up
```

FF!Test will be available at http://localhost/iaf-test

### Running a different version
 
It is possible to run the test image using a different version. 
This can be done by exporting the `VERSION` property. For example:

```shell
export VERSION=7.9-SNAPSHOT
```

The `.env` file should contain the current version of the project, but it can be changed for a more permanent solution. 

### Running Larva tests

There are two scenario root directories that can be used. 

- **embedded testtool directory /opt/frank/testtool**: Uses copied tests, the image needs to be rebuild if changes are made. 
- **external testtool directory /opt/frank/testtool-ext**: Uses mounted tests, which can be changed while the image is running.
