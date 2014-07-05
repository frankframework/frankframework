Ibis AdapterFramework
=====================

Build adapters using XML configuration. Build application using adapters.

![Ibis AdapterFramework](IAF.png)

Small XML configuration example which defines an adapter:

	<adapter name="HelloWorld" description="Little example">
		<receiver className="nl.nn.adapterframework.receivers.GenericReceiver" name="HelloWorld">
			<listener className="nl.nn.adapterframework.receivers.JavaListener" name="HelloWorld"/>
		</receiver>
		<pipeline firstPipe="HelloWorld">
			<exits>
				<exit path="EXIT" state="success"/>
			</exits>
			<pipe name="HelloWorld" className="nl.nn.adapterframework.pipes.FixedResult" returnString="Hello World">
				<forward name="success" path="EXIT"/>
			</pipe>
		</pipeline>
	</adapter>



Releases
========

See [release notes](RELEASES.md).



Mailing list and IRC
====================

The Ibis community can be contacted via
https://groups.google.com/d/forum/ibissource. You can join this mailing list by 
sending a message to ibissource+subscribe@googlegroups.com. Or try to find
somebody online on IRC using the
[web interface](http://irc.codehaus.org/?channels=ibis&uio=d4) or an
[IRC client](irc://irc.codehaus.org/ibis).



Eclipse
=======

- Download
  [Eclipse Kepler SR2](http://eclipse.org/downloads/packages/eclipse-ide-java-ee-developers/keplersr2)
- Unzip and start Eclipse.
- Close Welcome.
- Make sure Maven is able to access the internet. E.g. when behind a proxy:
  Window, Preferences, Maven, User Settings, settings.xml should exist and
  contain proxy configuration.
- Window, Open Perspective, Other..., Git, OK, Clone a Git repository,
  URI: https://github.com/ibissource/iaf.git, Next, Next, Finish.
- Right click iaf, Import projects..., Next, Finish.
- Window, Open Perspective, Other..., Java EE.
- Servers, No servers are available. Click this link to create a new server...,
  Apache, Tomcat v7.0 Server, Next, Browse..., select the root folder of a
  Tomcat installation (when not available download
  [Tomcat](http://tomcat.apache.org/) (version 7.0.22 is known to work, but
  other version are expected to work too)), OK, Finish.
- Double click Tomcat v7.0 Server at localhost, Open launch configuration,
  Arguments, VM arguments, add -Dapplication.server.type=TOMCAT6, OK, Modules,
  Add Web Module..., iaf-example, OK, File, Save
- Right click Tomcat v7.0 Server at localhost, Start.
- Browse the IAF console at
  [http://localhost:8081/iaf-example/](http://localhost:8081/iaf-example/).



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

After modifying a project file:

- ctrl-c
- cd .. ; mvn clean install ; cd example ; mvn jetty:run
