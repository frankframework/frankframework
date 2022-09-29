To use the Oracle database, you first need to create a base image using a Dockerfile from https://github.com/oracle/docker-images/tree/main/OracleDatabase/SingleInstance/dockerfiles

Use the following command to build an image for the Oracle database with the testuser.

	docker build -t iaf-oracle .

Next, start the Oracle container by using the docker-compose.yml file using:

	docker-compose up -d

The -d flag is used to run the container in the background.

It will take a while for the Oracle container to have started, you can check the status with "docker ps" and the container should say (healthy)
If you want to remove the container you can use "docker-compose down", if you just want to stop the container without removing it use "docker-compose stop" and use "docker-compose up" to start it up again.

You will need to add the following to your context.xml file, which can be found at iaf\test\src\main\webapp\META-INF\, to connect to the database in the container.

	<Resource
		name="jdbc/ibis4test-oracle"
		factory="org.apache.naming.factory.BeanFactory"
		type="oracle.jdbc.xa.client.OracleXADataSource"
		URL="jdbc:oracle:thin:@host.docker.internal:1521:XE"
		user="${testiaf_user/username:-testiaf_user}"
		password="${testiaf_user/password:-testiaf_user00}"
	/>
