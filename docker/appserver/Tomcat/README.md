# Running Ibis4Test within a Docker container

This subdirectory of the IAF Git repository lets you run Ibis4Test within a Docker container. To run this project, please do the following:

- Run a dockerized Oracle database. You find instructions in subdirectory `iaf/test/src/main/tools/setupDB/Oracle`.
- Run the Maven build of the IAF project. You can do this concurrently with starting the Oracle database if you want. Use the following command:

      mvn -P oracle,tomcat,docker clean install

- Wait until the Oracle database container has initialized users and tables. When this happens, it still takes some time before you can connect to the database. Also wait for the Maven build to finish; it creates the docker image you need.
- Create and run your ibis4test container with the following command:

      docker run --name ibis4test -p80:80 iaf-test-as-tomcat:7.6-SNAPSHOT

- In your browser, find the Frank!Framework at http://localhost/iaf-test. Run the Larva tests in the Frank!Console.