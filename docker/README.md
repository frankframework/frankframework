# Frank!Framework - Docker

This module contains Docker images for various appservers to test the Frank!Framework with.
The Docker images have been customized to run without needing much configuration.
The images contain the test configurations and secrets to connect with
our [auxiliary images](https://github.com/ibissource/iaf-ci-images) which include the databases and MQs.

This folder also contains a docker compose file to make it easy to run.

## Building test image

Use Maven to build the project and the Docker image

```shell
mvn clean install -P docker,Tomcat
```

## Running test image

First make sure that you can access the auxiliary images. This can be done by building them yourself, or by using the
prebuild images in the private Docker registry `private.docker.nexus.frankframework.org`. The private repository
requires [login](https://docs.docker.com/engine/reference/commandline/login/).

Use Docker compose to run any combination of test image, database and MQ. The properties will be resolved automatically.
For example, to start Tomcat with the default H2 in-memory database:

```shell
docker compose -f tomcat.yml up
```

Combine the YAML files in this folder to start a more enhanced test environment.
For example, if you want to run WildFly with MariaDB and Artemis, run this command:

```shell
docker compose -f wildfly.yml -f mariadb.yml -f artemis.yml up
```

FF!Test will be available at http://localhost/iaf-test

## Debugging inside a Docker image

It is possible to attach your Java debugger to a Docker image. You can place breakpoints but also live update classes to
work on fixes easily, without the whole Maven build and Docker compose cycle.

- First, build the basic image as described above with Maven, to build the test image.
- Second, start the image with the following command, from the current 'docker' folder:
  ```shell
  docker compose -f tomcat-debug.yml up
  ```
  This is equivelent to:
  ```shell
  export VERSION=7.9-SNAPSHOT
  docker run --rm \
	-e JPDA_ADDRESS=8001 -e JPDA_TRANSPORT=dt_socket \
	-p 80:8080 -p 8001:8001 \
	-v ./../test/src/test/testtool:/opt/frank/testtool-ext \
	-v ./../core/target/ibis-adapterframework-core-$VERSION.jar:/usr/local/tomcat/webapps/iaf-test/WEB-INF/lib/ibis-adapterframework-core-$VERSION.jar:ro \
	ff-test:$VERSION-tomcat \
	/usr/local/tomcat/bin/catalina.sh jpda run
  ```
- Third, attach to your running process, inside your IDE. In IntelliJ, choose `Remote JVM Debug` and use port 8001 at
  localhost.
- You can use breakpoints and if you update/save/compile a class inside your IDE, it is automatically updated inside
  your attached Docker image. Unfortunately, this
  has [limitations](https://www.jetbrains.com/help/idea/altering-the-program-s-execution-flow.html#hotswap-limitations).
- Hint: the above (optional) line with `-v` links your locally build core.jar file to the Tomcat instance. So, upon
  an image restart, you get the latest code. By performing `mvn package -pl core -am -DskipTests` in the root of your
  project, you have a new `core.jar` to use, once you restart your Docker image. This method is possible with all jar
  files used by FF!.

### Running a different version

It is possible to run the test image using a different version.
This can be done by exporting the `VERSION` property. For example:

```shell
export VERSION=7.9-SNAPSHOT
```

The `.env` file should contain the current version of the project, but it can be changed for a more permanent solution.

### Running Larva tests

There are two scenario root directories that can be used.

- **embedded testtool directory /opt/frank/testtool**: Uses copied tests, the image needs to be rebuild if changes are
  made.
- **external testtool directory /opt/frank/testtool-ext**: Uses mounted tests, which can be changed while the image is
  running.
