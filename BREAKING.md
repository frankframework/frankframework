Contains breaking changes per release.

10.2
--------------
[Commits](https://github.com/frankframework/frankframework/compare/release/10.1...HEAD)

* In ticket [10862](https://github.com/frankframework/frankframework/issues/10862), `JdbcTransactionalStorage` has been modified to not be able to create tables anymore, this should be done by using the liquibase migrations. 
  This was deprecated since 7.9.0. 

NOTE: previously, we put this information in RELEASES.md. Take a look there for older breaking changes and migration notes.
