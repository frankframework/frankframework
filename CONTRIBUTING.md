# How to contribute

I'm really glad you're reading this, because we need volunteer developers to help this project come to fruition.

If you haven't already, come find us on our [community forums](https://ibissource.org/forum). We want you working on things you're excited about.



## Running the IBIS Adapter Framework

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



## Submitting changes

Please send a [GitHub Pull Request](https://github.com/ibissource/iaf/pull/new/master) with a clear list of what you've done (read more about [pull requests](https://help.github.com/articles/about-pull-requests/)). When you send a pull request, make sure all of your commits are atomic (one feature per commit).

Always write a clear log message for your commits. Add the current year, the name of the copyright owner and the copyright notice (when not already present) to adjusted and new files. See:

- https://www.gnu.org/licenses/gpl-howto.html
- https://www.gnu.org/prep/maintain/html_node/Copyright-Notices.html
- https://opensource.stackexchange.com/questions/2367/how-do-i-properly-specify-the-years-of-copyright
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

One-line messages are fine for small changes, but bigger changes should look like this:

    $ git commit -m "A brief summary of the commit
    > 
    > A paragraph describing what changed and its impact."



## Coding conventions

Start reading our code and you'll get the hang of it. We optimize for readability:

  * We indent using tabs, not spaces.
  * We ALWAYS put spaces after list items and method parameters (`[1, 2, 3]`, not `[1,2,3]`) and around operators (`x += 1`, not `x+=1`).
  * This is open source software. Consider the people who will read your code, and make it look nice for them. It's sort of like driving a car: Perhaps you love doing donuts when you're alone, but with passengers the goal is to make the ride as smooth as possible.
  * Use Unix style newlines.



## Developing with Eclipse

- Download and unzip
  [Eclipse Kepler SR2](http://eclipse.org/downloads/packages/eclipse-ide-java-ee-developers/keplersr2)
  (64-bit Eclipse doesn't work with 32-bit JRE/JDK (doesn't start without any
  message)).
- Start Eclipse with Java 7. You might want to
  [use -vm in eclipse.ini](http://wiki.eclipse.org/Eclipse.ini#Specifying_the_JVM).
- Close Welcome.
- Make sure that the default text file line delimiter is set to Unix and
  default encoding is set to UTF-8: Window, Preferences, General, Workspace,
  New text file line delimiter: Unix, Text file encoding: UTF-8.
- Make sure Maven is able to access the internet. E.g. when behind a proxy:
  Window, Preferences, Maven, User Settings, settings.xml should exist and
  contain proxy configuration.
- Window, Open Perspective, Other..., Git, OK, Clone a Git repository,
  URI: https://github.com/ibissource/iaf.git, Next, Next, Finish.
- Optionally (when you have access to the proprietary jars some modules depend
  on) also clone:
  URI: https://bitbucket.org/ibissource/mvn-repo-proprietary.git, User: ...,
  Password: ..., Next, Next, Finish. 
- Right click iaf, Import projects..., Next, **deselect**: iaf-coolgen, iaf-ibm,
  iaf-ifsa, iaf-sap and iaf-tibco (unless you cloned mvn-repo-proprietary),
  Finish.
- Window, Open Perspective, Other..., Java EE.
- Servers, No servers are available. Click this link to create a new server...,
  Apache, Tomcat v7.0 Server, Next, Browse..., select the root folder of a
  Tomcat installation (when not available download
  [Tomcat](http://tomcat.apache.org/) (version 7.0.22 is known to work, but
  other version are expected to work too)), OK, Finish.
- Double click Tomcat v7.0 Server at localhost, Open launch configuration,
  Arguments, VM arguments, add -Dotap.stage=LOC, OK, Modules, Add Web Module...,
  iaf-example, OK, File, Save
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
  (not a JRE) is used (use Java 1.6 to compile with the minimal Java version for
  the IAF project), Refresh, Refresh resources upon completion,
  Specific resources, Specify Resources..., iaf (Using "The project containing
  the selected resource" doesn't seem to work), Finish, Run.
- The local Maven repository might contain corrupt jar files which for example
  will result in java.lang.NoClassDefFoundError:
  org/aspectj/lang/ProceedingJoinPoint when starting Tomcat. Remove the jar file
  from the repository to make Maven download the file again.
- When changing IAF versions Eclipse doesn't always automatically clean the 
  tomcat deploy folder (wtpwebapps). Rightclick project, Run As, Maven Clean, followed by 
  Right click Tomcat v7.0 Server at localhost, Clean...
- When running Tomcat v8.5, make sure to disable its pluggability scans to prevent unnecessarily long startup times.
  To do this, go to Tomcat's local context.xml file (under the Servers folder in Eclipse's project explorer) and
  add a JarScanner element. Within the JarScanner's tags, add a self-closing JarScannerFilter element with the defaultPluggabilityScan="false" attribute.


## Developing with IntelliJ

- Clone this any way you like. E.g. at the commandline: git clone git@github.com:ibissource/iaf.git
- File -> Open project, and select the pom.xml which just appeared.
- To use git via intellij you need to install the git and/or github plugin.
- You can add a tomcat configuration via Run-> Edit Configuration -> + -> Tomcat Server -> Local -> Add example webapp under deployments tab.
- Run it


Thanks,
The IAF Team
