Ibis AdapterFramework release notes
===================================

[Tags](https://github.com/ibissource/iaf/releases)



Upcoming
--------

[JavaDocs](http://www.ibissource.org/iaf/maven/apidocs/index.html)
[Commits](https://github.com/ibissource/iaf/compare/v5.5...HEAD)
[![Build Status](https://travis-ci.org/ibissource/iaf.png)](https://travis-ci.org/ibissource/iaf)

- Add support for jetty-maven-plugin
- Added functionality to forward form fields as sessionKeys (in RestListeners)
- Added possibility to write log records (to separate log files) without the message itself (e.g. for making counts)
- Configuration warning when FxF directory doesn't exist
- Added parameter pattern 'uuid' (which can be used instead of the combination of 'hostname' and 'uid')



5.6.1
---

[Commits](https://github.com/ibissource/iaf/compare/v5.6...v5.6.1)
[![Build Status](https://travis-ci.org/ibissource/iaf.png?branch=v5.6.1)](https://travis-ci.org/ibissource/iaf)

- Fixed bug in EsbSoapWrapper where Result element was inserted instead of Status element 



5.6
---

[Commits](https://github.com/ibissource/iaf/compare/v5.5...v5.6)
[![Build Status](https://travis-ci.org/ibissource/iaf.png?branch=v5.6)](https://travis-ci.org/ibissource/iaf)

- Move missing errorStorage warning from MessageKeeper (at adapter level) and logfile to ConfigurationWarnings (top of console main page).
- Replace (broken) enforceMQCompliancy on JmsSender with MQSender.
- Remove FXF 1 and 2 support.
- Fix Ibis name and DTAP stage in Bootstrap theme.
- Add theme switch button.
- Add stream support to FilePipe and FileSender.
- Add permission rules to FileViewerServlet.
- Added the possibility for enabling LDAP authentication and authorization without a deployment descriptor
- Added functionality for unit testing (TestTool)
- Added some MS SQL support
- Extended functionality for MoveFilePipe and CleanupOldFilesPipe, and introduced UploadFilePipe
- Introduction of SimpleJdbcListener; activate pipeline based on a select count query
- Added possibility to process zipped xml files with a BOM (Byte Order Mark)
- Added locker functionality to pipeline element (it was already available for scheduler element)

### Non backwards compatible changes

- Attribute enforceMQCompliancy on JmsSender has been removed, use nl.nn.adapterframework.extensions.ibm.MQSender instead of nl.nn.adapterframework.jms.JmsSender when setTargetClient(JMSC.MQJMS_CLIENT_NONJMS_MQ) is needed.
- Support for FXF 1 and 2 as been dropped.



5.5
---

[Commits](https://github.com/ibissource/iaf/compare/v5.4...v5.5)
[![Build Status](https://travis-ci.org/ibissource/iaf.png?branch=v5.5)](https://travis-ci.org/ibissource/iaf)

- Also when not transacted don't retrow exception caught in JMS listener (caused connection to be closed and caused possible other threads on the same listener to experience "javax.jms.IllegalStateException: Consumer closed").
- Tweaked error logging and configuration warnings about transactional processing.
    - Show requirement for errorStorage on FF EsbJmsListener as configuration warning instead of log warning on every failed message.
    - Removed logging error "not transacted, ... will not be retried" and warning "has no errorSender or errorStorage, message ... will be lost" (when a listener should run under transaction control (ITransactionRequirements) a configuration warning is already shown).
    - Removed logging error "message ... had error in processing; current retry-count: 0" (on error in pipeline an appropriate action (e.g. logging) should already been done).
    - Don't throw RCV_MESSAGE_TO_ERRORSTORE_EVENT and don't log "moves ... to errorSender/errorStorage" when no errorSender or errorStorage present.
    - Removed some unused code and comments like ibis42compatibility.
    - Renamed var retry to manualRetry for better code readability.
- Prevent java.io.NotSerializableException when the application server wants to persist sessions.
- Prevent problems with control characters in Test Tool GUI (replace them with inverted question mark + "#" + number of character + ";").
- Alpha version of new design Ibis console.
- Better support for Active Directory and other LdapSender improvements.
    - Make "filter" on LDAP error/warning messages work for AD too.
    - Added unicodePwd encoding.
    - Added changeUnicodePwd operation.
    - Added challenge operation to LdapSender (LdapChallengePipe has been deprecated).
    - Made it possible to specify principal and credentials as parameters.
    - Set errorSessionKey to errorReason by default.
    - Cleaned debug logging code and exclude password from being logged.
- Fixed a lot of javadoc warnings.
- Introduction of XmlFileElementIteratorPipe; streamed processing of (very large) xml files
- Better integration of Maven and Eclipse (using Kepler SR2).
- added "Transaction Service" to console function "Show Security Items", and added configuration warning "receiver/pipeline transaction timeout exceeds system transaction timeout"



5.4
---

[Commits](https://github.com/ibissource/iaf/compare/v5_3...v5_4)
[![Build Status](https://travis-ci.org/ibissource/iaf.png?branch=v5.4)](https://travis-ci.org/ibissource/iaf)

- First steps towards running IBISes on TIBCO AMX (as WebApp in Composite)
- added "Used SapSystems" to console function "Show Security Items"
- prevent OutOfMemoryError in console function "Adapter Logging" caused by a lot of files in a directory
- added facility to hide properties in console functions "Show configuration" and "Show Environment variables"
- Fixed problems with XSD's using special imports.
- Removed unused code which generates a NPE on JBoss Web/7.0.13.Final.
- Replace non valid xml characters when formatting error message.
- Made it possible to add http headers when using a HttpSender.
- Fixed exception in file viewer when context root of IAF instance is /.
- Bugfix addRootNamespace.
- Made it possible to override the address location in the generated WSDL.
- Possibility to dynamically load adapters.



5.3
---

[Commits](https://github.com/ibissource/iaf/compare/v5_2...v5_3)

- Better DB2 support.
- Some steps towards making a release with Maven.
- First steps towards dynamic adapters.
- Specific java class, which returns Tibco queue information in a xml, is extended with more information.
- On the main page of the IBIS console ("Show configurationStatus") for each RestListener a clickable icon is added (this replaces separate bookmarks).
