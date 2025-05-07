# Testing with IAF-Test

To ensure that your contribution doesn't break any logic, we would like you to run the test scenario's within the iaf-test module before committing your changes. To do this, a number of extra dependencies are required. These are placed as runtime-dependencies in the `test/pom.xml` but the overview is placed below for verification, and in case something goes wrong with the dependency management.

This guide was written with the assertion that you are A) using Eclipse, and B) have successfully ran the iaf-example module before. If this is not the case, please follow the steps as described on our [CONTRIBUTING](https://github.com/frankframework/frankframework/blob/master/CONTRIBUTING.md#developing-with-eclipse) page. For users of IntelliJ, see chapter 3.

## 1. Proprietary modules and JAR dependencies

Download the following JAR files. We advise you to place them in the Servers\lib folder of your Eclipse workspace. If you don't have this folder, you can create it.
If you use IntelliJ, open "Settings | Build, Execution, Deployment | Application Servers", locate your Tomcat application server, and add these libraries:
* [jakarta.management.j2ee-api-1.1.4](https://mvnrepository.com/artifact/jakarta.management.j2ee/jakarta.management.j2ee-api/1.1.4)
* [jakarta.jms-api-3.1.0](https://mvnrepository.com/artifact/jakarta.jms/jakarta.jms-api/3.1.0)
* [jakarta.transaction-api-2.0.1](https://mvnrepository.com/artifact/jakarta.transaction/jakarta.transaction-api/2.0.1)

If you want to use Queuing or a DBMS other than H2, you need to ensure the corresponding JDBC drivers are in place:
* For MariaDB or MySQL: [mysql-connector-java-8.0.20.jar](https://dev.mysql.com/downloads/connector/j/) (N.B. for proper XA support, the MySQL driver is used for MariaDB DBMS too)
* For MSSQL           : [mssql-jdbc-7.2.2.jre8.jar](https://docs.microsoft.com/en-us/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server)
* For Oracle          : [ojdbc8.jar](https://www.oracle.com/database/technologies/appdev/jdbc-ucp-183-downloads.html)
* For PostgreSQL      : [postgresql-42.2.14](https://jdbc.postgresql.org/download/postgresql-42.2.14.jar)
* For ActiveMQ        : [activemq-all-5.8.0.jar](https://mvnrepository.com/artifact/org.apache.activemq/activemq-core/5.8.0)

> [!WARNING]\
> Versions are subject to change, please check our [pom.xml](https://github.com/frankframework/frankframework/blob/master/pom.xml) for the current driver(s) and their corresponding versions.

In Tomcat's launch configuration (go to the Java EE perspective to access your Tomcat server, find launch configuration in the Tomcat Overview window), go to the Classpath tab. Click on the User Entries item and click on the [ Add JARs... ] button. Select all JARs in the lib folder, press OK, and press OK again.

Download the latest [LTS of NodeJS](https://nodejs.org/en) needed to build the frontend.

## 2. Tomcat configuration - Eclipse

The module's test scenarios can be run manually with the Larva testtool. This will be done within an iaf-test instance running on your Tomcat server. To make this possible...

1. In the Project Explorer, go to your Tomcat's _catalina.properties_ file. At the bottom, add the lines:

   - `log.dir=c:/temp` (lower case 'c' is mandatory)
   - `dtap.stage=LOC`.
   - `credentialFactory.class=org.frankframework.credentialprovider.PropertyFileCredentialFactory`
   - `credentialFactory.map.properties=<path to your sources root>/test/src/main/secrets/credentials.properties`
   - `authAliases.expansion.allowed=testalias`
2. In the Tomcat Overview window, set the port number for HTTP/1.1 to 80. If you wish to use another port, please set the property `web.port` accordingly in catalina.properties.
3. In the same window, go to the Modules tab at the bottom and add "/iaf-test" as a web module.
4. In the Project Explorer, right-click the iaf-test module and select Properties. Go to Deployment Assembly, press [ Add... ]. Select Folder, press [ Next ]. Select the src/main/configurations folder, and press Finish. In the text field right of your new src/main/configurations item, enter `WEB-INF/classes/configurations`. _(might not be necessary anymore)_
5. Do the same for the src/test/testtool folder. For that, enter `testtool` as deploy path. _(might not be necessary anymore)_

### Note about the log dir:
The log dir is also used as test data directory for the Larva test tool as it is a directory which is known to be writable.
Saving test results as expected output for future tests, also saves the log dir in file name paths. Therefore, it is important
for now not to change this directory.
If however you do not use Windows but a Unix based system (such as Linux or macOS), then set the `log.dir` property to whatever works
on your system, for instance `$HOME/ibis4test-logs` but be aware that you cannot save test-results and share that with others.

## 3. Create a Run Configuration - IntelliJ

Unfortunately it is not possible to provide a run configuration for IAF-Test in the repository, since the configuration contains system-dependent
paths, so you have to make your own Run Configuration for this. Be careful, because a few things can go wrong that can cause tests or the startup to fail.

If you have checked out multiple workspaces, you may wish to set different ports for HTTP and JMX in the RunConfiguration in each workspace. This allows you to start instances from each workspace in
parallel, so you can for instance run multiple debug sessions side by side to compare system-behaviour in a test in different branches.

### Create a Tomcat Run Configuration and set the following:
1. Select your installed Tomcat Application Server
2. Make sure that you have enabled the Maven profile `database-drivers` and reload the Maven project after enabling it.
2. Add the following parameters to the VM Options:
   - `-Ddtap.stage=LOC`
   - `-DauthAliases.expansion.allowed=testalias`
   - `-Dlog.dir=c:/temp` (lower case 'c' is mandatory) (or whatever works for your system, drive letters on Windows must be lowercase).
   - `-DcredentialFactory.class=org.frankframework.credentialprovider.PropertyFileCredentialFactory`
   - `-DcredentialFactory.map.properties=<path to your sources root>/test/src/main/secrets/credentials.properties`
3. Set the Tomcat HTTP port to `80`
NB: If you want to run on a different port, you also need to add to your VM options the option `-Dweb.port=8080` (or whatever port you chose).
4. In the tab "Deployments", select the module `frankframework-test: war exploded` and the application context-path `/iaf-test`.
5. Other settings as you find appropriate

### Note about the log dir:
See the Eclipse instructions for an important note about the `log.dir` setting.

## 4. Select database

The frankframework-test project supports multiple databases. By default, an H2 local database is used.
Docker projects for a number of other DMBSes are provided in GitHub project https://github.com/frankframework/ci-images. To use one of the provided databases, run the `rebuild.bat` script in the corresponding directory. (requires Docker to be installed on your machine). To configure the frankframework-test application, set in the catalina.properties or the VM Options the property `jdbc.dbms.default` to `oracle`, `mssql`, `mysql`, `mariadb` or `postgres`.

## 5. Running the test scenarios

### Eclipse
Run your Tomcat server from Eclipse's Servers view. It may take up to a minute for Eclipse to launch it; once ready, you can find the Frank!Framework console by browsing to http://localhost/iaf-test/.

### IntelliJ
Start your IAF-Test Run Configuration from the "Run Configurations" menu or from the "Services" tool panel, either in "Run" or in "Debug" mode.
Depending on how you configured it when the system is ready it will either open a browser window automatically, or you can manually navigate to http://localhost/iaf-test/ in your browser of choice.

### Starting the Tests
Once the Frank!Framework console is loaded, go to the Larva testtool in the sidebar. Specify which scenarios to run and under which conditions - the default settings should be good for checking if everything works.

Press [ Start ], sit back, relax, do some stretches, and let's hope for the best. :)

---

### Troubleshooting

* Some parts of the iaf-test module rely on proprietary modules. To tell Maven that it should download these modules, go to Window > Preferences > Maven > User Settings. If you already have a _settings.xml_ file, press the "Open file" link. Otherwise, browse to _C:/Users/(your name)/.m2/_ and create a _settings.xml_ file. Edit the file by adding your own repository or the [frankframework nexus repository](https://nexus.frankframework.org/content/groups/private/) as [mirror](https://maven.apache.org/guides/mini/guide-mirror-settings.html).
* When your IP-address is dynamically generated, you may have problems connecting to your database that runs in a docker image. The Larva tests reference the database host by a DNS name, `host.docker.internal`. Docker may not automatically update the IP address to which this name refers when your computer is assigned a new IP address. To see whether you have this issue, do `ping host.docker.internal` in a command prompt. If you cannot reach this address, refresh your docker network. You can do that using docker desktop. Press the settings icon (cogwheel) to the top. In the left-hand menu, choose Resources | Network. Update the docker subnet.
* If you are using an Oracle database, mind the version of the driver you download. For each combination of a JDK version and an Oracle database version, the driver is different. The filename on the Oracle download page depends only on the JDK version though. You can find the Oracle version we use in `frankframework-docker/pom.xml`.
