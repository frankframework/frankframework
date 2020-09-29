# Running Ibis4Test within a Docker container

This subdirectory of the IAF Git repository lets you run Ibis4Test within a Docker container. To run this project, please do the following:

- Find the IP address of your host, for example using `ifconfig` or `ipconfig`. You may have multiple IP addresses. This project was tested for IP address `192.168.178.11`. In the remainder of this file, read your own IP address for `192.168.178.11`.
- Run the Maven build of the IAF project with the following command:

      mvn -DdatabaseHost=192.168.178.11 -P oracle clean install

- Copy `iaf/test/target/ibis-adapterframework-test-<your version>.war to `iaf/test/src/main/tools/appServers/tomcat/iaf-test.war`.
- Run a dockerized Oracle database. You find instructions in subdirectory `iaf/test/src/main/tools/setupDB/Oracle`.
- In the mean time, build your ibis4test container of the Frank!Framework for using an Oracle database. Within the directory of this `README.md` file, please enter the following command:

      docker build --build-arg ORACLE_DB_IP=192.168.178.11 --tag ibis4test .

- Wait until the Oracle database container has initialized users and tables. When this happens, it still takes some time before you can connect to the database.
- Run your ibis4test container with the following command:

      docker run --name ibis4test -p80:80 ibis4test

- In your browser, find the Frank!Framework at http://localhost/iaf-test. Run the Larva tests in the Frank!Console.