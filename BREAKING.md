# Breaking changes

Contains breaking changes per release.

> [!NOTE]\
> Previously, we put this information in RELEASES.md. Take a look there for older breaking changes and migration notes.

10.0.4
--------------
* Tomcat has been upgraded to 11.0.29 for CVEs that have been fixed. As a side effect of this upgrade, JNDI resources defined for the application as `<Resource ... />`
  in the Context XML file now need to have a `type` attribute defined, or a the `object_name` attribute, or Tomcat will fail to start up with a `NullPointerException`.
  (NB: This file may be named `context.xml` but it can have different names)
* Due to tightening security restrictions in the Linux distribution on which our Tomcat Docker images are built, the default J2V8 JavaScript engine used in the `JavaSender` no
  long works out-of-the-box.
  Try changing to the `GraalJS` engine, or start Tomcat with the environment-variable `GCLIB_TUNABLES` set to `glibc.rtld.execstack=2`.
  Because of the security-implications we do not wish to bake in this into the Docker images we provide.
