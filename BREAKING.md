# Breaking changes

Contains breaking changes per release.

> [!NOTE]\
> Previously, we put this information in RELEASES.md. Take a look there for older breaking changes and migration notes.

10.4
--------------
[Commits](https://github.com/frankframework/frankframework/compare/release/10.3...HEAD)

* The Tomcat Docker image no longer lets the console's log viewer read `/opt/frank`: `FileViewer.permission.rules` in the image now only allows `/usr/local/tomcat/logs`, matching the default described in `SECURITY.md`.
  If you browse other files under `/opt/frank` through the Logging page, set `FileViewer.permission.rules` yourself, for example as a system property.
* `FileViewer.permission.rules` now matches whole path elements: a rule for `/opt/logs` no longer also matches `/opt/logs-old` or `/opt/logsecrets`. Add a rule per directory if you relied on the old prefix match.

10.3
--------------
[Commits](https://github.com/frankframework/frankframework/compare/release/10.2...HEAD)

* In ticket [11345](https://github.com/frankframework/frankframework/issues/11345) validation for the instance name was added. If you are using a name that is not valid the application will not start up.

10.2
--------------
[Commits](https://github.com/frankframework/frankframework/compare/release/10.1...HEAD)

* In ticket [10424](https://github.com/frankframework/frankframework/issues/10424), we have created a new way to configure a keystore and / or a truststore.
  Where you previously had to use something like `<HttpSender truststore="testTruststoreAttribute">`, you can now use `<truststore truststoreResource="testTruststore" />` as a sub-element of the applicable frank element.
  Both syntaxes are still supported, but the old syntax is deprecated and will be removed in a future release.
* In ticket [10862](https://github.com/frankframework/frankframework/issues/10862), `JdbcTransactionalStorage` has been modified to not be able to create tables anymore, this should be done by using the liquibase migrations.
  This was deprecated since 7.9.0.
* In ticket [10768](https://github.com/frankframework/frankframework/issues/10768), we have upgraded the rabbitmq dependency to 4.3.0, which implies that the new address format (v2) is used.
  This also means that the only thing a listener can listen to is a queue, and not an exchange anymore. If you want to listen to an exchange, you need to create a queue and bind it to the exchange.
  So perhaps you need to change some things on your RabbitMQ server, or change your configuration to use a queue instead of an exchange.
