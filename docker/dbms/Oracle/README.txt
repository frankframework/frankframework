First create a docker account and install Docker Desktop for your computer.
Go to https://hub.docker.com/ and log in using your docker account.
Search for the Oracle Database 12c Enterprise Edition docker image or go directly to https://hub.docker.com/_/oracle-database-enterprise-edition
Click on "Proceed to Checkout" on the right side of the page.
Fill in your Contact Information and check the required boxes and click "Get Content".
You are now able to download and use the Oracle Database docker image, so follow the rest of the instructions below.

Go to the directory containing your Dockerfile using a command line interface (such as PowerShell).
First, use the following command and log in using your docker account.

	docker login

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
		URL="jdbc:oracle:thin:@localhost:1521:ORCLCDB"
		user="testiaf_user"
		password="testiaf_user00"
	/>
