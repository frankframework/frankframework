Contains breaking changes per release.

10.2
--------------
[Commits](https://github.com/frankframework/frankframework/compare/release/10.1...HEAD)

* In ticket [10424](https://github.com/frankframework/frankframework/issues/10424), we have created a new way to configure a keystore and / or a truststore. 
Where you previously had to use something like `<HttpSender truststore="testTruststoreAttribute">`, you can now use `<truststore truststoreResource="testTruststore" />` as a subelement of the applicable frank element.
Both syntaxes are still supported, but the old syntax is deprecated and will be removed in a future release.
* In ticket [10862](https://github.com/frankframework/frankframework/issues/10862), `JdbcTransactionalStorage` has been modified to not be able to create tables anymore, this should be done by using the liquibase migrations. 
  This was deprecated since 7.9.0. 

NOTE: previously, we put this information in RELEASES.md. Take a look there for older breaking changes and migration notes.
