# Testing with IAF-Test

To ensure that your contribution doesn't break any logic, we would like you to run the test scenario's within the iaf-test module before committing your changes. To do this, you'll have to download a handful of JARs and adjust your Tomcat server configuration.

This guide was written with the assertion that you are A) using Eclipse, and B) have successfully run the iaf-example module before. If this is not the case, please follow the steps as described on our [CONTRIBUTING](https://github.com/ibissource/iaf/blob/master/CONTRIBUTING.md#developing-with-eclipse) page.

To prevent problems with data transactionality, we will be using an Oracle database rather than an H2 database. If you don't have Oracle Database Express Edition installed on your system, download it [here](https://www.oracle.com/technetwork/database/enterprise-edition/downloads/index.html). (Express Edition downloads can be found a bit further down on the page)

## 1. Proprietary modules and JAR dependencies

Some parts of the iaf-test module rely on proprietary modules. To tell Maven that it should download these modules, go to Window > Preferences > Maven > User Settings. If you already have a _settings.xml_ file, press the "Open file" link. Otherwise, browse to _C:/Users/(your name)/.m2/_ and create a _settings.xml_ file. Edit the file following [these instructions](https://knowhow.interpar.nl/?epkb_post_type_1=how-to-add-proprietary-to-your-settings-xml-of-maven).

Download the following JARs, and place them in your Tomcat server's lib folder. If you do not yet have a lib folder, you can create one directly under your Tomcat server's root directory.
* [activemq-core-5.6.0.jar](https://mvnrepository.com/artifact/org.apache.activemq/activemq-core/5.6.0)
* [geronimo-j2ee-management\_1.1_spec-1.0.1.jar](https://mvnrepository.com/artifact/org.apache.geronimo.specs/geronimo-j2ee-management_1.1_spec/1.0.1)
* [geronimo-jms\_1.1_spec-1.1.1.jar](https://mvnrepository.com/artifact/org.apache.geronimo.specs/geronimo-jms_1.1_spec/1.1.1)
* [geronimo-jta\_1.1_spec-1.1.1.jar](https://mvnrepository.com/artifact/org.apache.geronimo.specs/geronimo-jta_1.1_spec/1.1.1)
* [kahadb-5.6.0.jar](https://mvnrepository.com/artifact/org.apache.activemq/kahadb/5.6.0)
* [ojdbc7.jar](https://www.oracle.com/technetwork/database/features/jdbc/jdbc-drivers-12c-download-1958347.html)
* [service-dispatcher-1.5.jar](https://mvnrepository.com/artifact/org.ibissource/service-dispatcher)

In Tomcat's launch configuration (found in the Tomcat Overview window), go to the Classpath tab. Click on the User Entries item and click on the [ Add JARs... ] button. Select all JARs in the lib folder, press OK, and press OK again.

## 2. Tomcat configuration

The module's test scenarios can be run manually with the Larva testtool. This will be done within an iaf-test instance running on your Tomcat server. To make this possible...

1. In the Project Explorer, go to your Tomcat's _catalina.properties_ file. At the bottom, add the lines `log.dir=c:/temp` and `otap.stage=LOC`.
2. In the Tomcat Overview window, set the port number for HTTP/1.1 to 80, or another if 80 is already taken. Oracle will be using port 8080.
3. In the same window, go to the Modules tab at the bottom and add "/iaf-test" as a web module.
4. In the Project Explorer, right-click the iaf-test module and select Properties. Go to Deployment Assembly, press [ Add... ]. Select Folder, press [ Next ]. Select the src/main/configurations folder, and press Finish. In the text field right of your new src/main/configurations item, enter `WEB-INF/classes/configurations`.
5. Do the same for the src/test/testtool folder. For that, enter `testtool` as deploy path.

## 3. Ant builds

To make sure our database contains the data the tests need, we'll have to run some ant scripts. Navigate to _iaf-test/src/main/tools_ in your Project Explorer. Run the following files as ant builds, in order:
1. _/setupDir/setupDir.xml_
2. _/setupDB/Oracle/create___user.xml_
3. _/setupDB/Oracle/create___database.xml_

## 4. Running the test scenarios

Run your Tomcat server from Eclipse's Servers view. It may take up to a minute for Eclipse to launch it; once ready, you can find the Ibis console by browsing to http://localhost/iaf-test/.

Once the Ibis console is loaded, go to the Larva testtool. Specify which scenarios to run and under which conditions - the default settings should be good for checking if everything works.

Press [ Start ], sit back, relax, do some stretches, and let's hope for the best. :)

---

### Troubleshooting

* If the ant builds don't work due to a missing class error, try running it with another version of Java (can be set by pressing "Ant Build..." instead of "Ant Build").
* If the JdbcQueryListener has trouble starting due to a "table [ibisstore] does not exist" error, you may have started the server too quickly after stopping its last instance (<5s).
* If running Tomcat results in an error related to invalid Oracle credentials, try rerunning the _create___user.xml_ script.
