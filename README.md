Latest release
==============

Version 5.3:

- Better DB2 support.
- Some steps towards making a release with Maven.
- First steps towards dynamic adapters.
- Specific java class, which returns Tibco queue information in a xml, is extended with more information.
- On the main page of the IBIS console ("Show configurationStatus") for each RestListener a clickable icon is added (this replaces separate bookmarks).

[More info...](https://github.com/ibissource/iaf/compare/v5_2...v5_3)

Next version:

- First steps towards running IBISes on TIBCO AMX (as WebApp in Composite)
- added "Used SapSystems" to console function "Show Security Items"
- prevent OutOfMemoryError in console function "Adapter Logging" caused by a lot of files in a directory

[More info...](https://github.com/ibissource/iaf/compare/v5_3...HEAD)

[![Build Status](https://travis-ci.org/ibissource/iaf.png)](https://travis-ci.org/ibissource/iaf)



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

JavaDocs: http://www.ibissource.org/iaf/maven/apidocs/index.html

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
  [Eclipse Kepler](http://www.eclipse.org/downloads/packages/eclipse-ide-java-ee-developers/keplersr1)
- Unzip and start Eclipse.
- Window, Preferences, Team, Git, History, Follow Renames.
- In Git Repositories view clone https://github.com/ibissource/iaf.git
- Right click iaf, Import projects...
- In Navigator view right click pom.xml, Run As, Maven build..., Skip Tests,
  Run.
- Refresh the project, build problems should be resolved.
- When Tomcat has been added to the Servers view it should be possible to add
  the project to the server and start it up.



IntelliJ
========

(needs to be updated as maven-parent isn't used anymore)
- Clone this any way you like. E.g. at the commandline: git clone git@github.com:ibissource/maven-parent.git
- File -> Open project, and select the pom.xml which just appeared.
- To use git via intellij you need to install the git and/or github plugin.
- You can add a tomcat configuration via Run-> Edit Configuration -> + -> Tomcat Server -> Local -> Add example webapp under deployments tab.
- Run it 

to check out via intellij doesn't work very well because of lacking support for submodules: 
(see http://youtrack.jetbrains.com/issue/IDEA-64024)



Command-line interface
======================

git clone https://github.com/ibissource/iaf
cd iaf/core
mvn -DskipTests