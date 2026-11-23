Contains breaking changes per release.

> [!NOTE]\
> Previously, we put this information in RELEASES.md. Take a look there for older breaking changes and migration notes.

10.2.2
--------------
* Tomcat has been upgraded to 11.0.29 for CVEs that have been fixed. As a side effect of this upgrade, JNDI resources defined for the application as `<Resource ... />`
  in the Context XML file now need to have a `type` attribute defined, or a the `object_name` attribute, or Tomcat will fail to start up with a `NullPointerException`.
  (NB: This file may be named `context.xml` but it can have different names)
* Due to tightening security restrictions in the Linux distribution on which our Tomcat Docker images are built, the default J2V8 JavaScript engine used in the `JavaSender` no
  long works out-of-the-box.
  Try changing to the `GraalJS` engine, or start Tomcat with the environment-variable `GCLIB_TUNABLES` set to `glibc.rtld.execstack=2`.
  Because of the security-implications we do not wish to bake in this into the Docker images we provide.
[Commits](https://github.com/frankframework/frankframework/compare/release/10.2...HEAD)

10.2
--------------
[Commits](https://github.com/frankframework/frankframework/compare/release/10.1...HEAD)

* In ticket [10424](https://github.com/frankframework/frankframework/issues/10424), we have created a new way to configure a keystore and / or a truststore. 
Where you previously had to use something like `<HttpSender truststore="testTruststoreAttribute">`, you can now use `<truststore truststoreResource="testTruststore" />` as a subelement of the applicable frank element.
Both syntaxes are still supported, but the old syntax is deprecated and will be removed in a future release.
* In ticket [10862](https://github.com/frankframework/frankframework/issues/10862), `JdbcTransactionalStorage` has been modified to not be able to create tables anymore, this should be done by using the liquibase migrations. 
  This was deprecated since 7.9.0.
* In ticket [10768]((https://github.com/frankframework/frankframework/issues/10768)), we have upgraded the rabbitmq dependency to 4.3.0, which implies that the new address format (v2) is used.
This also means that the only thing a listener can listen to is a queue, and not an exchange anymore. If you want to listen to an exchange, you need to create a queue and bind it to the exchange.
So perhaps you need to change some things on your RabbitMQ server, or change your configuration to use a queue instead of an exchange.

NOTE: previously, we put this information in RELEASES.md. Take a look there for older breaking changes and migration notes.
