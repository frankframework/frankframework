# Testing with IAF-Test

To ensure that your contribution doesn't break any logic, we would like you to run the test scenario's within the iaf-test module before committing your changes. To do this, you'll have to download a handful of JARs and adjust your Tomcat server configuration.

This guide was written with the assertion that you are A) using Eclipse, and B) have successfully ran the iaf-example module before. If this is not the case, please follow the steps as described on our [CONTRIBUTING](https://github.com/ibissource/iaf/blob/master/CONTRIBUTING.md#developing-with-eclipse) page.

## 1. Proprietary modules and JAR dependencies

Download the following JAR files. We advice you to place them in the Servers\lib folder of your Eclipse workspace. If you don't have this folder, you can create it.
* [activemq-all-5.6.0.jar](https://mvnrepository.com/artifact/org.apache.activemq/activemq-core/5.6.0)
* [geronimo-j2ee-management\_1.1_spec-1.0.1.jar](https://mvnrepository.com/artifact/org.apache.geronimo.specs/geronimo-j2ee-management_1.1_spec/1.0.1)
* [geronimo-jms\_1.1_spec-1.1.1.jar](https://mvnrepository.com/artifact/org.apache.geronimo.specs/geronimo-jms_1.1_spec/1.1.1)
* [geronimo-jta\_1.1_spec-1.1.1.jar](https://mvnrepository.com/artifact/org.apache.geronimo.specs/geronimo-jta_1.1_spec/1.1.1)

If you want to use a DBMS other that H2, you need to add the corresponding JDBC driver:
* For MariaDB or MySQL: [mysql-connector-java-8.0.20.jar](https://dev.mysql.com/downloads/connector/j/) (N.B. for proper XA support, the MySQL driver is used for MariaDB DBMS too)
* For MSSQL           : [mssql-jdbc-7.2.2.jre8.jar](https://docs.microsoft.com/en-us/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server)
* For Oracle          : [ojdbc8.jar](https://www.oracle.com/database/technologies/appdev/jdbc-ucp-183-downloads.html)
* For PostgreSQL      : [postgresql-42.2.14](https://jdbc.postgresql.org/download/postgresql-42.2.14.jar)

In Tomcat's launch configuration (go to the Java EE perspective to access your Tomcat server, find your launch configuration in the Tomcat Overview window), go to the Classpath tab. Click on the User Entries item and click on the [ Add JARs... ] button. Select all JARs in the lib folder, press OK, and press OK again.

## 2. Tomcat configuration

The module's test scenarios can be run manually with the Larva testtool. This will be done within an iaf-test instance running on your Tomcat server. To make this possible...

1. In the Project Explorer, go to your Tomcat's _catalina.properties_ file. At the bottom, add the lines:

   - `log.dir=c:/temp` (lower case 'c' is mandatory)
   - `dtap.stage=LOC`.
   - `credentialFactory.class=nl.nn.credentialprovider.PropertyFileCredentialFactory`
   - `authAliases.expansion.allowed=testalias`
2. In the Tomcat Overview window, set the port number for HTTP/1.1 to 80. If you wish to use another port, please set the property `web.port` accordingly in catalina.properties.
3. In the same window, go to the Modules tab at the bottom and add "/iaf-test" as a web module (has tool-tip ibis-adapterframework-test).
4. In the Project Explorer, right-click the ibis-adapterframework-test project and select Properties. Go to Deployment Assembly, press [ Add... ]. Select Folder, press [ Next ]. Select the src/main/configurations folder, and press Finish. In the text field right of your new src/main/configurations item, enter `WEB-INF/classes/configurations`. _(might not be necessary anymore)_
5. Do the same for the src/test/testtool folder. For that, enter `testtool` as deploy path. _(might not be necessary anymore)_

## 3. Select database

The ibis-adapterframeworkt-test project supports multiple databases. By default an H2 local database is used.
Docker projects for a number of other DMBSes are provided in iaf/docker/dbms. To use one of the provided databases, run the `rebuild.bat` script in the corresponding directory. (requires Docker to be installed on your machine). To configure the ibis-adapterframeworkt-test application, set in the catalina.properties the property `jdbc.dbms.default` to `oracle`, `mssql`, `mysql`, `mariadb` or `postgres`.

## 4. Running the test scenarios

Run your Tomcat server from Eclipse's Servers view. It may take up to a minute for Eclipse to launch it; once ready, you can find the Ibis console by browsing to http://localhost/iaf-test/.

Once the Ibis console is loaded, go to the Larva testtool. Specify which scenarios to run and under which conditions - the default settings should be good for checking if everything works.

Press [ Start ], sit back, relax, do some stretches, and let's hope for the best. :)

---

### Troubleshooting

* Some parts of the iaf-test module rely on proprietary modules. To tell Maven that it should download these modules, go to Window > Preferences > Maven > User Settings. If you already have a _settings.xml_ file, press the "Open file" link. Otherwise, browse to _C:/Users/(your name)/.m2/_ and create a _settings.xml_ file. Edit the file by adding your own repository or the [frankframework nexus repository](https://nexus.frankframework.org/content/groups/private/) as [mirror](https://maven.apache.org/guides/mini/guide-mirror-settings.html).
* When your IP-address is dynamically generated, you may have problems connecting to your database that runs in a docker image. The Larva tests reference the database host by a DNS name, `host.docker.internal`. Docker may not automatically update the IP address to which this name refers when your computer is assigned a new IP address. To see whether you have this issue, do `pint host.docker.internal` in a command prompt. If you cannot reach this address, refresh your docker network. You can do that using docker desktop. Press the cog wheel to the top. In the left-hand menu, choose Resources | Network. Update the docker subnet.
* If you are using an Oracle database, mind the version of the driver you download. For each combination of a JDK version and an Oracle database version, the driver is different. The filename on the Oracle download page depends only on the JDK version though. You can find the Oracle version we use in `ibis-adapterframework-docker/pom.xml`.
* If initially the database is not available when Tomcat boots, the Frank!Framework's retry mechanism might fail. You may have to restart Tomcat in Eclipse.
