Ibis AdapterFramework
=====================

Build adapters using XML configuration. Build applications using adapters.

![Ibis AdapterFramework](IAF.png)

Some example XML configurations:
- [HelloWorld](example/src/main/resources/Example/ConfigurationHelloWorld.xml)
- [HelloWorlds](example/src/main/resources/Example/ConfigurationHelloWorlds.xml)
- [ManageDatabase](example/src/main/resources/Example/ConfigurationManageDatabase.xml)

See them in action: http://ibis4example.ibissource.org/


Releases
========

See [release notes](RELEASES.md).



Mailing list
============

The Ibis community can be contacted via
https://groups.google.com/d/forum/ibissource. You can join this mailing list by 
sending a message to ibissource+subscribe@googlegroups.com.



Eclipse
=======

- Download and unzip
  [Eclipse Kepler SR2](http://eclipse.org/downloads/packages/eclipse-ide-java-ee-developers/keplersr2).
- Start Eclipse with Java 7. You might want to
  [use -vm in eclipse.ini](http://wiki.eclipse.org/Eclipse.ini#Specifying_the_JVM).
- Close Welcome.
- Make sure Maven is able to access the internet. E.g. when behind a proxy:
  Window, Preferences, Maven, User Settings, settings.xml should exist and
  contain proxy configuration.
- Window, Open Perspective, Other..., Git, OK, Clone a Git repository,
  URI: https://github.com/ibissource/iaf.git, Next, Next, Finish.
- Optionally (when you have access to the proprietary jars some modules depend
  on) also clone:
  URI: https://bitbucket.org/ibissource/mvn-repo-proprietary.git, User: ...,
  Password: ..., Next, Next, Finish. 
- Right click iaf, Import projects..., Next, unless you cloned
  mvn-repo-proprietary deselect: iaf-coolgen, iaf-ibm, iaf-ifsa, iaf-sap and
  iaf-tibco, Finish.
- Window, Open Perspective, Other..., Java EE.
- Servers, No servers are available. Click this link to create a new server...,
  Apache, Tomcat v7.0 Server, Next, Browse..., select the root folder of a
  Tomcat installation (when not available download
  [Tomcat](http://tomcat.apache.org/) (version 7.0.22 is known to work, but
  other version are expected to work too)), OK, Finish.
- Double click Tomcat v7.0 Server at localhost, Open launch configuration,
  Arguments, VM arguments, add -Dlog.dir=C:\\\\Temp (or any other folder), OK,
  Modules, Add Web Module..., iaf-example, OK, File, Save
- Right click Tomcat v7.0 Server at localhost, Start.
- Browse the IAF console at
  [http://localhost:8080/iaf-example/](http://localhost:8080/iaf-example/).

In some cases you might want/need to:

- Rightclick iaf, Maven, Update Project..., OK.
- Enable Project, Build Automatically
- Right click Tomcat v7.0 Server at localhost, Clean...
- Change newlines in .classpath and org.eclipse.wst.common.component files
  back to Unix newlines.
- Rightclick pom.xml (in iaf), Run As, Maven build..., JRE, make sure a JDK
  (not a JRE) is used (use Java 5 to compile with the minimal Java version for
  the IAF project), Refresh, Refresh resources upon completion,
  Specific resources, Specify Resources..., iaf (Using "The project containing
  the selected resource" doesn't seem to work), Finish, Run.



IntelliJ
========

- Clone this any way you like. E.g. at the commandline: git clone git@github.com:ibissource/iaf.git
- File -> Open project, and select the pom.xml which just appeared.
- To use git via intellij you need to install the git and/or github plugin.
- You can add a tomcat configuration via Run-> Edit Configuration -> + -> Tomcat Server -> Local -> Add example webapp under deployments tab.
- Run it



Command-line interface
======================

Initial:

- git clone https://github.com/ibissource/iaf
- cd iaf
- mvn
- cd example
- mvn jetty:run
- [http://localhost:8080/](http://localhost:8080/)


After modifying a project file:

- ctrl-c
- cd .. ; mvn clean install ; cd example ; mvn jetty:run

The jetty-maven-plugin requires Maven 3 and Java 1.7.



Commits
=======

Add the current year, the name of the copyright owner and the copyright notice
(when not already present) to adjusted and new files. See:

- http://www.gnu.org/licenses/gpl-howto.html
- http://stackoverflow.com/questions/3487007/when-to-update-the-year-in-open-source-copyright-notice
- http://www.apache.org/licenses/LICENSE-2.0#apply

When relevant to the end-user first add a line to [release notes](RELEASES.md)
at the end of [section 'Upcoming'](RELEASES.md#upcoming) with a functional
description targeted to end-user. There's no limit on the length of this line.
After that create the commit message targeted to developers and Git tools with a
short one-line description in the first line (in some cases the same as the
release notes line) and if necessary a more detailed explanatory text after a
blank line. See:

- http://chris.beams.io/posts/git-commit/#seven-rules
- http://stackoverflow.com/questions/2290016/git-commit-messages-50-72-formatting
- http://git-scm.com/book/ch5-2.html

Use Unix style newlines.
