# Running Ibis4Test within a Docker container

This subdirectory of the IAF Git repository lets you run Ibis4Test within a Docker container. To run this project, please do the following:

- Find the IP address of your host, for example using `ifconfig` or `ipconfig`. You may have multiple IP addresses. This project was tested for IP address `192.168.178.11`.
- Run a dockerized Oracle database. You find instructions in subdirectory `iaf/test/src/main/tools/setupDB/Oracle`.
- Wait until the container has initialized users and tables. When this happens, it still takes some time before you can connect to the database.
- In the mean time, build your Oracle container for the Frank!Framework. Within the directory of this `README.md` file, please enter the following command:

      docker build --build-arg ORACLE_DB_IP=192.168.178.11 --tag ibis4test .

- Run your container with the following command:

      docker run --name ibis4test -p80:80 ibis4test

- The default version of the Frank!Framework is `7.6-20200915.172517`. If you want another version, then build the image with an extra argument: `--build-arg IAF_VERSION=<your version>`.