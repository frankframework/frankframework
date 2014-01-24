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


Communication
=============

The Ibis community can be contacted via
https://groups.google.com/d/forum/ibissource. You can join this mailing list by 
sending a message to ibissource+subscribe@googlegroups.com. Or try to find
somebody online on IRC a
[web interface](http://irc.codehaus.org/?channels=ibis&uio=d4) or
[IRC client](irc://irc.codehaus.org/ibis).

<iframe src="http://irc.codehaus.org/?channels=ibis&uio=d4" width="647" height="400">&nbsp;</iframe>



Eclipse
=======

Look at [README.eclipse](README.eclipse) for an explanation on how to build this in eclipse (needs te be updated).



How to make a release
=====================

I documented [here](RELEASE.md) how to make a release with mvn/git (needs to be updated).
