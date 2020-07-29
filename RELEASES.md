Ibis AdapterFramework release notes
===================================

[Tags](https://github.com/ibissource/iaf/releases)
[JavaDocs](https://javadoc.ibissource.org/latest/)



Upcoming
--------

[Commits](https://github.com/ibissource/iaf/compare/v7.5-RC1...HEAD)
[![Build Status](https://travis-ci.org/ibissource/iaf.png)](https://travis-ci.org/ibissource/iaf)


- reset Adapter Statistics by the hour when entering a new hour time slot
- introduced property jdbc.datasource.default, with default value jdbc/${instance.name.lc}
- introduced attribute combineBlocks on ForEachChildElementPipe, that can be set to false to leverage BlockEnhancedCapabilites of the configured sender
- Add possibility to put error details in ESB SOAP body response
- Improve MessageStoreListener by adding peekUntransacted attribute
- Add support for single pipeline SOAP 1.1 & 1.2 and REST JSON & XML, backed by XML-Schema validation, with OpenAPI 3.0 JSON Schema generation

### Non backwards compatible changes

- Change HashPipe attribute encoding into charset. Its default has changed from ISO8859-1 into UTF-8



7.5-RC1
--------

[Commits](https://github.com/ibissource/iaf/compare/v7.4...v7.5-RC1)
[![Build Status](https://travis-ci.org/ibissource/iaf.png?branch=v7.5-RC1)](https://travis-ci.org/ibissource/iaf)


- Make attribute firstPipe in PipeLine optional. When empty, the first Pipe in the Pipeline configuration
  is considedred to be the first. Similarly the success forward defaults to the next Pipe in the PipeLine.
- Enable to specify a namespace without a prefix in attribute namespaceDefs, to help simplify xpathExpressions e.g. into '/root/sub' instead of '/\*[local-name()='root']/\*[local-name()='sub'.
- Make ForEachChildElementPipe optionally streaming when using elementXPathExpression too. N.B. This requires an XsltProcessor that properly supports streaming. The versions of Saxon and Xalan that we currently employ do not; 
  Add options 'targetElement' and 'containerElement' to ForEachChildElementPipe to enable processing of very large files, avoiding memory problems that still exist with 
  xpath expressions for very large files;
  Make Xslt streaming default for xsltVersion=1
- Bugfix (un)loading configs in JmxMbeanHelper
- Add LadybugPipe
- Show Security Items: Improve error handling for 'Used JmsRealms' and 'Used Authentication Entries'
- Add Javascript Sender
- Refactor SchedulerHelper
- Refactor mediatypes in ApiListener
- Add MultipartHttpSender
- Add config warning when xsltVersion does not match detected version in xslt
- Automatically wire Spring beans into the IbisApplicationContext
- Fix header parameters being added to formand multipart-bodyparts
- Xslt pipe dynamic stylesheet
- Add logging and don't unload config when no database connection present
- ForEachChildElementPipe option to keep namespaces
- Add proxy attributes to ExchangeFolderSender
- Introduce LdapFindGroupMembershipsPipe
- Fix and improve (auto)reload in GUI 3.0
- Introduce SaxException, with clear message, and proper stacktrace under IBM JDK
- Fix stopCondition in ForEachChildElementPipe + remove count from result + introduce maxItems attribute
- Correct log instantiating using LogUtil instead of Logger
- Upgrade apache commons codec to the latest version
- Add proxy functionality to the SendGridSender
- Refactor SchedulerAdapter and remove jobs when config is unloaded
- Fix concurrency issues when a RestListener registers on a dispatcher
- Fix error when uploading configuration in GUI3.0
- Fix Classloader resource retrieval
- Create full path option for FileSystemListeners
- Remove BasePathClassLoader and refactor IConfigurationClassLoaders
- Add scroll to top in GUI 3.0
- Resolve relative directories for J2V8
- Add bounceAddress attribute on MailSender
- Fix and Cleanup MailSender and MailSenderBase
- Upgrade ladybug to version 2.0.9
- Namespace support for skip empty tags
- Base64 encode and decode option for FileSystemPipe, FileSystemSender and descendants 
- Add option to rotate by size, number of files or number of days to FileSystemPipe, FileSystemSender and descendants
- FileSystemSenders now return an InputStream when action=read
- XsltSender, XsltPipe, JsonXsltSender, JsonXsltPipe accept streaming inputs
- Add option in GUI 3.0 to cut off all traffic to Ladybug debugger, and to switch it on again
- Add option to use username and password from authAlias in Paramter values
- Bugfix autoReload new configuration on other nodes
- Support XML-Schema 1.1 for XML validation
- Detect character set of input streams by examining BOM
- Accept XML 1.1 valid characters in XML 1.0 documents while STAX parsing, to solve illegal XML problems in ExchangeMailListener.
- Rotate message.log by size instead of by day.
- Add character set to each outbound HTTP request.

### Non backwards compatible changes

- Make DirectoryListener extend FileSystemListener. It no longer supports attributes fileList, 
  fileListForcedAfter, outputFilenamePattern, passWithoutDirectory, numberOfBackups and random.
- Remove attribute 'count' from result of iterating pipes like ForEachChildElementPipe, to enable streaming output.
- The MailSender displayName element no longer exist, please use attribute `name` on the from/to elements instead.
- jdbc.convertFieldnamesToUppercase has been set to true by default
- FileSystemSenders with read action no longer encode base64 by default.
- HttpSender, WebServiceSender and descendants no longer support attribute xmlTag



7.4
--------

[Commits](https://github.com/ibissource/iaf/compare/v7.3...v7.4)
[![Build Status](https://travis-ci.org/ibissource/iaf.png?branch=v7.4)](https://travis-ci.org/ibissource/iaf)

- Improve validation config warnings
- ShowConfigurationStatus - improve error view
- Possibility to add new domains independently of ear file
    - UploadConfigService adapter
    - Facility to (de)activate configs
- Add Samba2Filesystem, used in SambaSender, SambaPipe and SambaListener
- Fix scheduler seconds/miliseconds bug
- Fix ability to run xslt1 xPath expressions with the xsltpipe and sender
- SAP clear function template cache at configuration reload
- Upgrade javassist dependency version to support java 8+
- Make sure FlowDiagrams are rendered upon ibis startup
- Add possibility for execute Oracle queries for H2 database



7.3
--------

[Commits](https://github.com/ibissource/iaf/compare/v7.2...v7.3)
[![Build Status](https://travis-ci.org/ibissource/iaf.png?branch=v7.3)](https://travis-ci.org/ibissource/iaf)

- Refactor CmisListener to an event based listener, you can now have multiple listeners listening to different events
- The cmis bridge functionality on the sender has been removed. In order to use the bridge you need to configure properties in the WAR/EAR file. See CmisSessionBuilder for more information about the properties that can be set
- Several bugfixes and performance improvements



7.3-RC1
--------

[Commits](https://github.com/ibissource/iaf/compare/v7.2...v7.3-RC1)
[![Build Status](https://travis-ci.org/ibissource/iaf.png?branch=v7.3-RC1)](https://travis-ci.org/ibissource/iaf)

- Generate IbisDoc and XSD and support beautiful configuration xml. The XSD can be used for code completion of beautiful Ibis configurations in Eclipse
- Use XSLT 2.0 instead of 1.0 for configuration tweaks (e.g. stub4testtool.xsl)
- Display showConfig jdbc errors per realm
- Add heath endpoints for individual adapters and entire application
- Add Google's V8 JavaScript engine
- Use VizJs to generate flow diagrams
- Improve 'error' handling in Show Security Items
- Upgrade Ladybug Test Tool to version 2.0.7
    - Make it possible to specify a transformation per report
    - Fix selecting root node on refresh at some parts of tree of Test tab
    - Return to previous active tab after closing tab
    - Show Compare button after run in Test tab
    - Display run result error (if any) on run in Test tab
    - Fix error on selecting checkpoint with null message
    - Fix error on selecting different stub strategy
- Add ability to use nullvalues on cmis properties
- Show class in IbisDoc html
- Show FileHandler properties in IbisDoc for FilePipe
- Add springIbisTestToolIgnoreReport.xml to ignore reports in Ladybug based on a session key
- Change IBIS startup servlet to CXF and register it with Spring
- Fix date issue where time properties are converted to GMT
- Revert old CMIS WS endpoints to register each service's WSDL individually
- Make WebServiceListener SOAP endpoint binding 1.2 capable
- Add option to add custom views to GUI 3.0
- Remove xslt2=true attribute, xslt version is now automatically detected. You may override this setting by specifying the xsltVersion attribute
- Fix potential memory leaks when:
    - NDC stacks are not cleaned up after processing messages
    - using the hideRegex attribute on pipes/adapters
    - consecutively processing multiple large messages in a row


### Non backwards compatible changes

- The dateformat in the CmisSender has been modified to `yyyy-MM-dd'T'HH:mm:ss.SSSZ` in order to use the old format, set the `jcmissender.processproperties.legacydateformat` property to true
- **Upgrade minimum require Java version from 6 to 7**. Java sources are still Java 1.6 compatible at this stage.



7.2
--------

[Commits](https://github.com/ibissource/iaf/compare/v7.1...v7.2)
[![Build Status](https://travis-ci.org/ibissource/iaf.png?branch=v7.2)](https://travis-ci.org/ibissource/iaf)

- Fix NPE in BatchFileTransformerPipe when using an IbisLocalSender
- Various bugfixes en performance improvements in SOAPProviders (WebServiceListener)
- Upgrade Ladybug Test Tool to version 2.0.6
    - Prevent error on reselect node after Delete and Replace
    - Refresh after upload
    - Limit the use of special chars in normalized path
    - Normalize path on save in report component
    - Don't show null in path label
    - Make report xml read-only in Edit mode
    - Show line numbers on report description too
    - Don't use TextArea for description in Read-only mode
    - Don't log all error messages to log file
    - Fix ClassCastException in Test tab for reports with description
    - Prevent losing typed data in edit mode on close or select node in tree
    - Copy report name from original report on Replace



7.1
--------

[Commits](https://github.com/ibissource/iaf/compare/v7.1-B4...v7.1)
[![Build Status](https://travis-ci.org/ibissource/iaf.png?branch=v7.1)](https://travis-ci.org/ibissource/iaf)

- Fix NPE in HttpSender when no charset is supplied in multipart response
- Modify GetFromSession to get key from input as well as sessionKey
- Refactor Base64Pipe to handle string, bytes and streams as both input and output
- Upgrade ibis-ladybug to latest version. See ladybug [releases.md](https://github.com/ibissource/ibis-ladybug/blob/master/RELEASES.md) for more info
- Add SambaSender to retrieve, upload and modify files through the SMB protocol
- Add parameters to JmsListeners to add JMS headers to the response message
- Add attribute to JmsSenders to read JMS headers and add them to the pipeline session
- Fix NPE in ErrorMessageFormatter when used with dynamic configurations
- Fix some bugs in CMIS error handling
- Update to latest Ladybug Test Tool version
    - Refactor code for errorLabel, okayLabel and getReport
    - Fix NPE on Open report (from Test tab), Edit, Save 
    - Show reports in child folders too in Test tab
    - Run reports in Test tab in background
    - Add (de)select all to Test tab
    - Make it possible to search case sensitive
    - Add ProgressBar to Test tab



7.1-B4
---

[Commits](https://github.com/ibissource/iaf/compare/v7.1-B3...v7.1-B4)
[![Build Status](https://travis-ci.org/ibissource/iaf.png?branch=v7.1-B4)](https://travis-ci.org/ibissource/iaf)


- Prevent poll guard to stop and start listener when recovering
- Upgrade Ladybug to fix rerun, title tag and rerun of saved test report
- Replace old Apache RPCRouter with CXF WebService provider
- Make rowlock optional in MS SQL prepareQueryTextForWorkQueueReading


### Non backwards compatible changes

- In order for CXF to use the Java standard WebServices it requires the `javax.xml.ws.WebServicePermission publishEndpoint` permission!



7.1-B3
---

[Commits](https://github.com/ibissource/iaf/compare/v7.1-B2...v7.1-B3)
[![Build Status](https://travis-ci.org/ibissource/iaf.png?branch=v7.1-B3)](https://travis-ci.org/ibissource/iaf)

- Many IAF GUI 3.0 improvements
- Fix Ladybug
- Add serviceNameSessionKey to IbisJavaSender so you can runtime determine the javaListener to dispatch to
- Fix Ibis-Struts which was missing some DTD files causing the xml parser to look those up online instead
- Add security logging for enabling/disabling Ladybug TestTool Report Generator
- Add poll guard which will stop and start the JMS listener when polling has stopped
- Make JMS receive timeout configurable
- Destroy JMS container when JMS listener is being stopped
- Restore destroy application context on full reload
- Fix CMIS CXF bus not using the IbisContext
- Fix NPE in IAF API when fetching errorstore count with a faulty database/table



7.1-B2
---

[Commits](https://github.com/ibissource/iaf/compare/v7.1-B1...v7.1-B2)
[![Build Status](https://travis-ci.org/ibissource/iaf.png?branch=v7.1-B2)](https://travis-ci.org/ibissource/iaf)

- Add updateEtag sessionKey to the ApiListener so it can be changed at runtime
- Modify HttpSender logging to only give warnings for 4xx (client errors) and 5xx (server errors)
- Fix javax.net.ssl.SSLSocketFactory.getDefault() SSLSocketFactory as default fallback when no keystore or truststore has been configured
- Fix IllegalDataException in ShowConfigurationStatus for non legal XML character in adapter message
- Add wss* attributes to SoapWrapperPipe
- Add configuration ClassLoader to SenderBase
- Fix NPE in larva testtool when trying to resolve nonessential property: configurations.directory
- Add overrideEntryPointWSDL property to CmisSender so the initial WSDL can be retrieved from the classpath opposed to a http call
- Add Boolean datatype to parameters
- Fix eTag handling bug in ApiListener
- Remove jmsRealm from IAF_Util, and make IAF_Util depend on the default configuration in which a database must be specified.
- Add WebAppClassLoader as default configuration classloader
- Add forgotten struts2rest services to web.xml LoginFilter
- Upgrade Bitronix to version 3.0.0-MK1
    The latest version has fixed the possiblity to set a dataSource bean on poolingDataSource and a connectionFactory bean on poolingConnectionFactory instead of using driverProperties.
    As well as many other bugfixes and improvements, see https://github.com/bitronix/btm for more info.
- LCM update quartz scheduler to use a slightly newer version
    Version 1.7.2 requires java 1.5.x, which is probably the reason the older version 1.6.6 was used. Version 1.6.6 is not hosted on maven central, which is the reason for this LCM upgrade.
    For the Quartz release notes see: https://web.archive.org/web/20100721184042/http://quartz-scheduler.org:80/
- Remove custom ibis modules
    These modules were introduced due to java 4 and 5 compilation issues and runtime issues on WAS4 and WAS5.
- Mavenize Ladybug


7.1-B1
---

[Commits](https://github.com/ibissource/iaf/compare/v7.0-RC3...v7.1-B1)
[![Build Status](https://travis-ci.org/ibissource/iaf.png?branch=v7.1-B1)](https://travis-ci.org/ibissource/iaf)

- Move 'Dynamic parameters' from showConfiguration to showEnvironmentVariables
- Show provided JmsDestinations with usage in showSecurityItems
- Introduction of the HashPipe
- Replace configurations.<config-name>.throwConfigNotFoundException property with configurations.<config-name>.configNotFoundReportLevel (possible values: ERROR (default), WARN, INFO, DEBUG)
- Make ibisstore checks capable for MS SQL (next to Oracle)
- Add throwJavaListenerNotFoundException attribute to IbisLocalSender
- Support for converting json+map to xml, to support REST UPDATEs
- Add duration statistics to showSchedulerStatus
- Upgrade CmisSender to support 'browser' bindings, support proxys and secure connections
- Refactor HttpSender and WebServiceSender and (LSM) upgrade HttpClient from version 3 to version 4. Better (Mtom-)Multipart implementation and support for other http implementations using the HttpSenderBase and HttpResponseHandler
- Fix NPE DumpIbisConsole
- Refactor CheckReload job
- StreamPipe: add possibility to append reason to AntiVirus Fail Message
- Add sessionKeyXPath attribute to Parameter (to be more flexible)
- Refactor ReplacerPipe replaceNonXmlChar and fix javadoc
- Introduction of the ShadowSender, a shadowSender allows you to duplicate the outgoing message and send it to an alternative endpoint for later use and comparison
- Support Unicode supplementary characters (like a smiley) in replace/stripNonValidXmlCharacters (which is used in ReplacerPipe). In the old code a Unicode supplementary character like a smiley was seen as two characters which would both be replaced/stripped. To be backwards compatible the Unicode supplementary characters are still replaced/stripped (by one character instead of two) but can be allowed using allowUnicodeSupplementaryCharacters
- Fix missing service client registering after stopping and starting rest/api adapters
- Workaround to avoid NPE after a full reload
- Avoid warning when ShowFlowDiagram REST listener is unavailable
- Support document(), xsl:import, and xsl:include with config in database
- Improve error handling TransformerPools and TransformerFactories
- Add support for configuration hierarchies. Configurations can define a parent configuration via the property 'configurations.<config-name>.parentConfig=<parentConfig-name>'
- Fix property file classloading with suffix files.The classloader will search and consolidate all properties found in the classloader chain
- Introduction of the RestSender
- Fix NPE in ApiEhCache after a full reload
- Replace struts page "Show configuration status" by rest service
- Add fetch action for CmisSender in order to retreive metadata
- Add CMIS bridge functionality (CmisListener) and the option to remap properties on getObject requests. A bridged cmisListener has to be configured to route traffic!
- Replace struts page "Show configuration" by rest service
- Add concurrency limit to parallel senders
- Encode file path parameter in FileViewerServlet
- Fix getResourceURL not being able to fetch files from places other then the classpath. This has now been disabled by default, but can be enabled with the property: classloader.allowed.protocols followed by the protocol(s) you wish to enable (file,http,ftp)
- Add console warning when security constraints (in web.xml file) are disabled. Suppress warnings by setting security.constraint.warning to false
- Fix CmisListener not being able to process multivalue properties
- Refactor all DbmsSupport classes
- Change webdav library to provided scope as it is incompatible with java 6
- Fix httpclient3 errors in IAF submodules
- Fix NPE in httpSender when retrieving result without content
- Remove unused Struts (validation) files
- Replace webdav library with custom implementation
- Add boolean support for CmisSender create/update actions
- Add possibility to send redirect in HttpSender
- Add regex attribute to XmlIf pipe
- Fix verifyHostname and allowSelfSignedCertificates bug in HttpSenderBase
- Remove jdk13Compatibility property in HttpSenderBase
- Replace struts page "Show Environment variables" by rest service
- Bugfix namespaces in XmlBuilder
- Introduction of JsonWellFormedChecker
- Introduction of Stream2StringPipe
- Fix stale connections not being cleaned up by the ConnectionPoolManager. See: https://issues.apache.org/jira/browse/HTTPCLIENT-1609
- Fix multipart parsing issues
- Update to latest Ladybug Test Tool version
    - Add custom XmlDecoder for testtool imports
    - Display escaped characters with different background color
    - Support Unicode supplementary characters (don't handle a smiley as two characters)
    - Change colors to be IAF GUI 3.0 friendly
- Prevent Show Tibco Queues to still fail when passive servers in server url
- Fix servlets in web.xml causing errors when cmis dependency is missing
- Bugfix error "ShowConfigurationStatus.xsl line [683]: Cannot convert string to double"
- Recover job: restart RestListeners of which uriPattern is not registered correctly
- Restore "IBIS startup failed" error page for ShowConfigurationStatus
- Bugfix multiple WebServiceSender and MTOM related issues
- Add IAF-TEST module for IAF integration tests
- Update to latest Ladybug Test Tool version
    - Bugfix pipe description from old configuration (before reload) being showed
    - Release old configuration objects when reloaded (new object was only used when pipe description not in cache)
    - Bugfix pipe description being showed from other configuration when both configurations contain the same adapter and pipe name
    - Show resources from schema, wsdl, fileName and schemaLocation attributes too in pipe description
- Add LarvaPipe
- Fix sub-resources eTag handling for ApiListener
- Add wssPasswordDigest attribute to WebServiceSender
- Add collectFileContents(Base64Encoded) to UnzipPipe


### Non backwards compatible changes

- ADDITIONAL.PROPERTIES.FILE.SUFFIX has been made obsolete. Users are adviced to remove suffix files as soon as possible.



7.0-RC3
---

[Commits](https://github.com/ibissource/iaf/compare/v7.0-RC2...v7.0-RC3)
[![Build Status](https://travis-ci.org/ibissource/iaf.png?branch=v7.0-RC3)](https://travis-ci.org/ibissource/iaf)

- Refactor ReplacerPipe replaceNonXmlChar and fix javadoc
- Support Unicode supplementary characters (like a smiley) in replace/stripNonValidXmlCharacters (which is used in ReplacerPipe). In the old code a Unicode supplementary character like a smiley was seen as two characters which would both be replaced/stripped. To be backwards compatible the Unicode supplementary characters are still replaced/stripped (by one character instead of two) but can be allowed using allowUnicodeSupplementaryCharacters
- Support document(), xsl:import, and xsl:include with config in database
- Fix NPE in ApiEhCache after a full reload
- Update to latest Ladybug Test Tool version
    - Bugfix pipe description from old configuration (before reload) being showed
    - Release old configuration objects when reloaded (new object was only used when pipe description not in cache)
    - Bugfix pipe description being showed from other configuration when both configurations contain the same adapter and pipe name
    - Show resources from schema, wsdl, fileName and schemaLocation attributes too in pipe description



7.0-RC2
---

[Commits](https://github.com/ibissource/iaf/compare/v7.0-RC1...v7.0-RC2)
[![Build Status](https://travis-ci.org/ibissource/iaf.png?branch=v7.0-RC2)](https://travis-ci.org/ibissource/iaf)

- Ignore import with namespace but without schemaLocation (fix on previous commit 'Support schema attribute with config in database and refactor ClassUtils.getResourceURL()')
- Rewrite dependencies on removed URL fallback in ClassUtils.getResourceURL()
- Make it possible to disable lastModifiedDelta in CleanupOldFilesPipe
- Fix WebServiceListener.processRequest() not being called by ServiceDispatcher (fix on previous commit 'Refactor http package to use IPipeLineSession instead of Map...')
- Fix cache not returning valueXPath transformed value in first call
- Add possibility to cache and share transformers (to save memory)
- Make ParallelSenders (with XsltSender) thread safe
- Add in memory Ldap server (for testing purpose)
- Drastically improved GUI 3.0 performance
- Fix recursive scheduleworker thread name



7.0-RC1
---

[Commits](https://github.com/ibissource/iaf/compare/v7.0-B3...v7.0-RC1)
[![Build Status](https://travis-ci.org/ibissource/iaf.png?branch=v7.0-RC1)](https://travis-ci.org/ibissource/iaf)

- Add support for integer and boolean parameters when using QuerySenders
- Fix broken "Show configuration warnings only at relevant configuration"
- Show config name for configuration errors and warnings
- Add possibility to skip configuration when not found in database (instead of error)
- Fix jsonpipe adding root elements when direction is xml2json
- Fix JdbcQuery, TestService and SendJmsMessage pages to log messages when sec.log.includeMessage=true
- First steps towards unit testing without application server
- Add attribute soapBodyNamespace to WsdlXmlValidator 
- Add Akamai Sender module
- Support multiple configuration directories for DirectoryClassLoader
- Add rootDir property to AkamaiSender
- Introduction of DummyClassLoader
- Add support for SNAPSHOT versions in Upload Configuration
- IbisTest: support for multiple scenarios root directories, use of project.basedir property
- Improved possibility to use a wildcard in sessionkeys parameters
- IsUserInRolePipe: add possibility to use pipe input as role
- Add attribute ignoreSoapFault to SoapWrapperPipe
- Add Liquibase migration support
- Add support for Antivirus scan parts in StreamPipe
- Add webapp index redirectproxy
- Add skipBOM support when outputType is stream in FileSender and FilePipe
- Fix databaseClassloading now use DbmsSupport to support other databases
- Fix Spring IntrospectionException when loading a databaseconfig which contains a FixedQuerySender
- Add flow diagram to Google Chrome, this was disabled due to preformance issues
- Bugfix UploadConfig - method 'startsWith' not supported in IE
- Fix unexpected value 'bodycontent' encountered' on Jboss
- Fix JdbcFacade.applyParameters to allow all types of input streams (instead of only FileInputStream and ByteArrayInputStream)
- Prevent string incompatible output types for file sender
- Add GUI 3.0 Download and activate database configuration button
- Enable action read for files on classpath and not available on the filesytem for FileSender and FilePipe
- Fix Show Tibco Queues: Skip exceptions from passive servers in server url
- Fix ReloadSender so it can be used as a standalone sender
- Init webapp.realpath and project.basedir earlier to make it possible to create a reference to it in the value of flow.create.url
- Add IBIS jdbc reconnect functionality on startup
- Fix redundant Quartz scheduler + memoryleak when ibis is reconnecting
- Add per config NotFoundException property instead of a global property
- Add HttpSenderBase to send http messages via httpcomponents 4+
- Fix NetStorageSender proxy authentication settings
- Fix ApiListenerServlet not always able to throw a proper exception
- Fix database classloader initiating as webapp classloader when no config can be found in the database
- Support schema attribute with config in database and refactor ClassUtils.getResourceURL()
- Add ServerSpecifics and CompanySpecifics properties
- Fix mailSender always trying to include attachments
- Add JsonXsltPipe
- Add Spring config for Tomcat in combination with Tibco



7.0-B3
---

[Commits](https://github.com/ibissource/iaf/compare/v7.0-B2...v7.0-B3)
[![Build Status](https://travis-ci.org/ibissource/iaf.png?branch=v7.0-B3)](https://travis-ci.org/ibissource/iaf)

- Add json to xml and xml to json conversion as well as json validation to xmlvalidators
- Prevent XML Entity Expansion (XEE) injection
- Upgrade ibis-servicedispatcher to version 1.4
- Hide strings in logging for error stack traces too
- ShowConfigurationStatus: Uniform Exceptions layout with Warnings
- ShowConfigurationStatus: Add link for 'alert' feature
- Add attribute mtomContentTransferEncoding to HttpSender (bugfix for MTOM)
- Add attribute version to JsonPipe
- Add dynamic throttling in senders
- Add property jdbc.convertFieldnamesToUppercase
- Add attribute hideRegex to pipes (next to global property)
- Fix UUIDGeneratorPipe when stubbed remove attribute type
- Add httpRequest and httpResponse sessionkeys to WebServiceListener
- Add facility to put outputValidator in inputValidator (to save memory)
- Show "Test a PipeLine" pipeline logging (bugfix)
- Hide strings in IBIS console messages too (same as in logging)
- Move config warning "Element not in list of available root elements" to startup
- Larva: add possibility to overwrite windiff command 
- Add IbisWebService constraint to /rest/* endpoint
- Remove "webContent.dir" bean from spring configuration
- Larva: add possiblity to autosave TestTool differences
- Fix stackoverflow on regex validation with Xerces parser
- Add attachment sessionKey attribute to MailSender



7.0-B2
---

[Commits](https://github.com/ibissource/iaf/compare/v7.0-B1...v7.0-B2)
[![Build Status](https://travis-ci.org/ibissource/iaf.png?branch=v7.0-B2)](https://travis-ci.org/ibissource/iaf)

- Fix IFSA no longer gives a warning when the managed connection factory can't be found
- Add consumes and produces option to rest endpoints to set mediatypes, this also transforms the data from and to JSON/XML when set
- Fix GUI 3.0 securityItems page didn't display specialSubjects properly
- Add ibisTester role to ldap loginFilter
- Add rest and rest-public endpoints to loginFilter
- Add exitCodes to pipeline exits, currently only work as statuscode for REST endpoints
- Add integer type to FixedQuerySender
- Fix made DefaultMessageListenerContainer configurable for Jms102
- Remove geronimo-jms_1.1_spec from runtime
- Bugfix in "Show Security Items - Used Authentication Entries", and add sorting (no duplicates)
- Larva: add support for multipart
- Add attribute mtomEnabled to HttpSender (to support MTOM in requests)
- Introduction of the pipe IfMultipart
- Add attribute elseForwardOnEmptyInput to IsXmlIfPipe
- Add attributes extractFirstStringPart and multipartXmlSessionKey to StreamPipe (to support multipart) 
- Fix splitting messagingLayer made configurable in EsbSoapWrapper
- Add commons-validator dependency for Jboss servers
- Fix fieldnames in query result are now automatically capitalized
- Add possibility to disable validators in stubbed mode
- Larva: add silent mode
- Introduction of 'Jdbc Properties'
- Fix jsp error for older servlets when retreiving the ServletContext
- Fix soap:address location used rest/webservices instead of servler/rpcrouter
- Add log messages state to "Show configuration status: *ALL*"
- Fix log.level now set automatically dependant on otap.stage
- Add senders to the MSG log
- Improve check on duplicate messages in MessageStoreSender
- Avoid NPE in "Show Scheduler Status"
- Add IbisTester role to IBIS LoginFilter
- Add queueConnectionFactoryName to XmlJmsBrowserSender possible input elements
- Make xmlStreamWriter in ScanTibcoSolutionPipe use central 
- Create XMLStreamWriter with XmlUtils.OUTPUT_FACTORY in all classes (ScanTibcoSolutionPipe didn't use XmlUtils.OUTPUT_FACTORY yet)
- Set log.dir automatically for Vanilla/Eclipse Tomcat too
- Add testtool.enabled property to enable the testtool on ACC and PRD
- Add facility in "Show configuration status: *ALL*" to show only 'alerted' adapters
- Bugfix in "Show EnvironmentVariables": "Application Constants" should not be resolved
- Avoid StackOverflowError in caching validators
- Fix performance on Oracle databases by providing more accurate path finding hints
- Add MqttListener
- Upgrade servlet version from 2.3 to 2.5
- Add attribute logIntermediaryResults to AbstractPipe to overwrite value in AppConstants
- Add attributes ignoreCaching and lazyInit to XmlValidator to overwrite value in AppConstants
- Add SNC encryption to SAP Listeners/Senders
- Add httpWsdl attribute to JavaListener for making WSDL available
- Make unwrapping of SOAP messages independent of SOAP version
- Support all InputStreams for upload in HttpSender instead of only FileInputStream
- Add MqttSender
- Fix DomainTransformerPipe nullPointerException on ibis startup
- Add custom SSLSocketFactory to CmisSender
- Fix JDBC driver default date format to yyyy-MM-dd
- Fix JDBC driver default timestamp format to yyyy-MM-dd HH:mm:ss
- Bugfix in ShowIbisstoreSummary "(SQLServerException) SQLState [S00010], errorCode [195]: 'to_char' is not a recognized built-in function name." 
- Add DllServiceDispatcher see [ibis-servicedispatcher](https://github.com/ibissource/ibis-servicedispatcher/commit/f759f897b063757bcc7a50229715035159d79dd5)
- Bugfix in ShowIbisstoreSummary (caused 2014-11-26)
- Fix connection leak in DomainTransformerPipe
- Prevent 'sap.jcoDestination does not exist'
- Fix NPE at startup trying to determine webContent.dir
- Remove property sec.log.enabled, security log is enabled by default
- Add support for dynamically setting the directory to UnzipPipe
- Add RESTFUL Etag caching to validate messages before they are processed by the IBIS
- Add IRestEtagCache interface and etag.cache.type property to choose between different caching solutions
- Add option to use Memcached as etag caching server
- Fix made IAF backwards compatible with ibis-servicedispatcher v1.3



### Non backwards compatible changes

- The default JDBC timestamp value has been set to yyyy-MM-dd HH:mm:ss, to disable this set the jdbc.timestampFormat property to an empty value
- The default JDBC date value has been set to yyyy-MM-dd, to disable this set the jdbc.dateFormat property to an empty value
- The security log has been activated by default. It is no longer possible to disable the security log!



7.0-B1
---

[Commits](https://github.com/ibissource/iaf/compare/v6.1...v7.0-B1)
[![Build Status](https://travis-ci.org/ibissource/iaf.png?branch=v7.0-B1)](https://travis-ci.org/ibissource/iaf)

- Replace Apache Commons Collections library v3.2 by v3.2.2
- Don't temporarily move already temporarily moved messages
- Replace struts form "Test a PipeLine" by rest service
- Add update entryName facility to LdapSender
- Bugfix in MessageStoreListener "(SQLServerException) SQLState [S0001], errorCode [102]: Incorrect syntax near '+'"
- Support multiple configurations and reading it with other classloader
- Add possibility to use a wildcard in sessionkeys parameters
- Replace struts form "Call an IFSA Service" by rest service (and default deactivate it)
- Bugfix "reload configuration" doesn't work for RR JmsListener
- Add configurations.files property and support configuration file in subfolder
- Add RemoveCacheKeyPipe
- Change Larva windiffCommand (make it relative to scenariosRootDirectories)
- Bugfix "reload configuration" loads all scheduled jobs again (next to the existing ones)
- Sort jobs in "Show Scheduler Status"
- Change Larva default timeout from 30 seconds to 5 seconds
- Update to latest Ladybug Test Tool version
    - Fix ibistesttool.defaultView
    - Support IAF change to support multiple configurations
- Refactor in response to Ladybug multiple configurations support and testing
    - Use properties.hide for configuration showed by Ladybug
    - Improve example configuration
    - Fix classloader related issue in loadConfigurationFile
    - Cache original and loaded configuration
    - Add getConfiguration to Adapter
    - Add getIbisManager to Configuration
- Make it possible to stop and start all adapters per configuration
- Add "*ALL*" link in "Show configuration status"
- Add attribute transformNullMessage to PipeLine
- Add logging about message to send in JMSFacade
- Add threadTopic attribute to MailSender for correlating mails in mail viewer
- Introduction of RhinoPipe (for running javascript)
- Introduction of WsdlGeneratorPipe (IJA_API specific)
- Show configuration warnings only at relevant configuration
- Visualize and remember selected configuration
- Add "*ALL*" link in "Show configuration"
- Bugfix "attribute [cacheMode] already has a default value [CACHE_CONSUMER]"
- Add facility to upload and show configurations in database (default disabled)
- Prevent NPE in console in case of configuration errors
- Allow /larva/* by TesterAccess only
- Add otap.stage=LOC to README (make Larva work)
- Add PublicAccess security-constraint (make Tomcat behave like WebSphere)
- Bugfix deriving soapAction for new ESB standard (without ServiceContext)
- Bugfix retrieving physical TIBCO queue when recovering
- Use log.trace instead of log.debug for REST services
- Add DatabaseClassLoader
- Improve special class loaders error handling
- Instantiate all query senders with Spring (make Spring aware of all database actions) (make defaultDataSource of example webapp work for all database actions)
- Add hideMethod attribute for masking strings in ErrorStore and MessageLog 
- Add hostname property to AppConstants properties
- Improve PassordHash with PBKDF2WithHmacSHA1 after consulting security (SHA1 is not a problem in the context of PBKDF2, see OWASP.org)
- Add roundsSessionKey attribute PasswordHashPipe and set to default value for Rounds to 40.000
- Add replyNotFound attribute to LdapSender
- Add sqlType 'ddl' to XmlQuerySender
- Change defaults for configurations.names, move IAF config to core jar
- Add default directory for DirectoryClassLoader, add webapp.realpath property
- Move configuration reload function from configuration servlet to IBIS console
- Split configuration reload in configuration reload and full reload
- Add messages about (configuration) startup and shutdown to IBIS console below summary
- Fix shutdown
- First steps towards generating flow images real time
- Larva: Add .diffType
- Upgrade commons-fileupload (fix file name always in lower case)
- Get configuration name from file name when configuration is uploaded
- Store configuration version and file name too when configuration is uploaded
- Add TimeOutException to IbisLocalSender (next to regular SenderException)
- Fix xml/url escaping of configuration name in showConfiguration(Status)
- Check ResultSetHoldability because of WebSphere + DB2 default
- Add proxiedDestinationNames
- Improve flexibility of Spring context file selection (add application.server.type.custom)
- Add support for WebSphere Liberty Profile
- Larva: addAbsolutePathProperties after substVars
- Show classloader type in "Show configuration status"
- Bugfix removing unused output namespaces in SoapWrapperPipe
- Add version to "Upload Configuration" and add derivative for name and version from file name
- Bugfix security log file for rest service "Test a PipeLine"
- Use correct configuration in "Show Environment variables"
- Add javaListenerSessionKey to IbisLocalSender
- Add facility to use separate properties files for each splitted configuration
- Add version and startup time to Application and Configuration logging in console
- Sort both tables in "/rest/webservices" on name
- Bugfix layout "Security Role Bindings" in console function "Show Security Items"
- Add lookupDestination attribute to JMSFacade to have possibility to connect to queues directly instead of via JNDI
- Add ability to log info from incoming messages on EsbJmsListener extracted by an xPath
- Change defaults for msg.log.level.default to terse, msg.log.hidden.default to true
- Add default logging conversationId from incoming messages on EsbJmsListener
- Add facility to generate flow images real time
- Bugfix in XmlIf for testing empty input
- Add namespaceDefs attribute to XmlSwitch
- Disable monitoring by default. It's usage is mostly replaced by log management tools like Splunk and Logstash. When monitoring is enabled keep in mind that it will cause OutOfMemory exceptions after a number of configuration reloads
- Prevent OutOfMemory exception after a number of configuration reloads
- Add parameter types "map" and "list" to Larva TestTool
- Add possibility to use zip files with multiple jar files in Upload Configuration
- Bugfix "fakepath" in Upload Configuration when using Chrome web browser
- Call close on JNDI context (InitialContext)
- Bugfix 'Browse a Jdbc table' for 'MS SQL for WebSphere'
- Add facility to generate flow images by CommandSender next to HttpSender
- Bugfix "facility to hide properties" caused by splitting configuration
- Introduction of CounterSwitchPipe (for load balancing between pipes)
- Add mime multipart response support to http sender
- Wait indefinitely for adapter to start when job interval is 0
- Add timeout field to IBIS console function 'Test a PipeLine'
- Add 'multiple configs' checkbox field to Upload Configuration
- Bugfix "Lock request time out period exceeded" for MS SQL
- Introduction of IsXmlIfPipe
- Add replaceFixedParams attribute to FixedResult (for easily replace multiple string in one pipe)
- Make Misc stream and reader related code continue when read returns 0
- Add resultStatusCodeSessionKey attribute to HttpSender (to store http status code from result)
- Introduction to new IBIS API + IBIS console
- Replace Apache XMLbeans library v2.5.0 by v2.6.0
- Add attribute parallel to iterating pipes
- Add log4j.properties for libraries like Spring
- When log.dir not specified try to set it automatically
- Fix xmlValidator.lazyInit caused by multiple configuration support
- Fix NPE in SenderWrapper caused by multiple configuration support
- Add xmlValidator.maxInitialised to limit memory usage by validators
- Remove unused artifacts
- Change IBIS API to use preconfigured endpoints
- Add property 'console.active' to enable the IBIS API/console
- Add IAF Upgrade notification when new version is available
- Add "webContent.dir" property to AppConstants properties
- Add attribute type to GetFromSession
- Fix optional headers in WSDL generator
- Extended classes for API management
- Add fileNameSessionKey to FixedResult pipe
- Replace links by tabs in "Show configuration", "Show configuration status" and "Show Environment variables"
- Ladybug: Prevent NPE with regexFilter when new thread is started (e.g. with ParallelSenders)
- Fix soapAction.xsl now works regardless of possible namespaces
- Fix SendTibcoMessage now uses soapAction.xsl to derive the default soapAction
- Add adapterToRunBeforeOnEmptyInput attribute to PipeLine
- Add locker (with ignoreTableNotExist) to job CleanupDatabase
- Sort tabs in "Show configuration", "Show configuration status" and "Show Environment variables"
- Add IBIS API endpoints: Browse JDBC, Execute JDBC queries, view serviceListeners, view securityItems
- Add option to disable IBIS Console idleTimeout functionality
- Add IBIS API docs to view all API endpoints
- Add IAF upgrade notification when new IAF version has been released
- Change "Show configuration status: *ALL*" to summary page (with improved performance)
- Add active checkbox to "Upload Configuration"
- Bugfix Base64 encoding/decoding not working in JdbcQuerySender when streaming to servlet
- Add interactive favicon for new GUI
- Larva: show diffMessage (hidden) for "scenario passed/failed"
- Larva: add httpRequest parameter
- StreamPipe: add httpRequest parameter (for passing filestreams to the backend)
- HttpSender: add multipartXmlSessionKey attribute
- Add possibility to skip the retrieval of multipart http requests to sessionKeys (in RestListeners)
- Bugfix in generating flow images for moving files
- Use the Sun Java Streaming XML Parser (SJSXP) as StAX implementation on all Application Servers
- Add CORS headers to /rest-public/ endpoints
- Add replaceNullMessage attribute to Adapter
- Add CheckReload adapter (with auto reload)
- Fix Jsonpipe to add roottag when non is present
- Add Feedback button to GUI 3.0

### Non backwards compatible changes

- The IBIS console functions "Call an IFSA Service" and "Show Monitors" have been deactivated. To activate them again add properties active.ifsa=true and monitoring.enabled=true



6.1
---

[Commits](https://github.com/ibissource/iaf/compare/v6.0...v6.1)
[![Build Status](https://travis-ci.org/ibissource/iaf.png?branch=v6.1)](https://travis-ci.org/ibissource/iaf)

- Equalize/refactor file name determination for FilePipe/Sender (for action "read" also consider attribute fileName and for action "info" also consider attributes fileName and classpath)
- Add CrlPipe
- Document JdbcTransactionalStorage type attribute
- Add storeResultAsStreamInSessionKey to HttpSender
- Remove IbisLocalSender and IbisJavaSender from pipes package (some time ago deprecated and moved to senders package)
- Add soapAction attribute and parameter to SendTibcoMessage
- Replace 'text' and 'plain' links in IBIS console function "Adapter logging" by 'bin' links
- Add attribute emptyInputReplacement to IExtendedPipe
- Add attributes skipOnEmptyInput, ifParam and ifValue to FixedForwardPipe
- Add value '!false' (equals 'true') to attribute active
- Add PasswordHashPipe
- Add check on queue existence in GetTibcoQueues and SendTibcoMessage (currently used in IJA_TiBeT2)
- Introduction of ExchangeMailListener
- Bugfix not showing length of blob fields in "Browse a Jdbc table" in case of multiple records
- Improve javadoc for MessageStoreSender/Listener
- Create directory in UploadFilePipe if it doesn't exist
- Upgrade Spring from release 2.5.6SEC03 to 3.2.16
- Update to latest Ladybug Test Tool version
    - Fix checkpoints not visible for uploaded reports
    - Add rerun functionality (principal, okay message, getEmptyInputReplacement)
- Bugfix schemaSessionKey in XmlValidator not working (caused in v6.0-RC1)
- Support for new ESB standard (without ServiceContext)
- Bugfix ignoreUnknownNamespaces default not true when noNamespaceSchemaLocation is being used
- Don't add namespace to schema by default when targetNamespace present and default namespace is not
- Determine default application server type (and remove version from type)
- Add configuration warning to use FixedQuerySender instead of DirectQuerySender
- Install a Servlet Filter to protect the Struts 1 Servlet from ClassLoader Manipulation attacks
- `(end of v6.1-RC1)`
- Improve IBIS console function "Show Adapter Statistics"
- Add attribute simple to ExchangeMailListener to save memory
- Copy functionality to temporarily move and/or chomp received messages for memory purposes to pipes (next to receivers)
- Add age of EMS server in GetTibcoQueues files (currently used in IJA_TiBeT2)
- Introduction of LdapFindMemberPipe
- Suppress DirectQuerySender configuration warning when called from IAF sources
- Introduction of CmisSender
- Make springCustom.xml obsolete to load springIbisTestTool[name].xml
- Remove username and password from springCustom.xml example
- Remove credentials from CredentialCheckingPipe output
- Remove obsolete files in WEB-INF
- Move REST services from default security role IbisWebService to IbisObserver
- Update to latest Ladybug Test Tool version
    - Make use of AppConstants properties which are now provided by IAF
    - Support new IAF feature to make springCustom.xml obsolete to load springIbisTestTool[name].xml
    - Add springIbisTestToolApi.xml
    - Merge springIbisTestTool.xml and springIbisTestToolTibet2.xml from IJA_Tibet2 (solve different rerunRoles on echo2Application in springIbisTestTool.xml in a different way)
    - Disable "Rerun didn't trigger any checkpoint" check when report generator is not enabled
    - Fix scope for instances of Views and View which implements BeanParent and should be prototype. Because View was singleton the isChangeReportGeneratorEnabledAllowed() call in TibetView was called on the wrong Echo2Application instance
- Add possibility to write a record with specified sessionKeys to security log file after a successful pipe run 
- `(end of v6.1-RC2)`
- Bugfix growing thread names (in logging and Ladybug TestTool)
- Change xsd schemaLocations in spring files to classpath protocol to prevent 'failed to read schema document'
- Replace "/servlet/rcprouter" by "/rest/webservices"
- Add masking for ErrorStore and MessageLog
- Add message to security log for 'Test a PipeLine'
- Upgrade from Java 5 to Java 6
- `(end of v6.1-RC3)`


### Non backwards compatible changes

- Don't add namespace to schema by default when targetNamespace present and default namespace is not. This is probably rarely the case. It doesn't make sense to change the default value in this case (only). Explicitly set addNamespaceToSchema to true when needed
	- ``src-resolve.4.1: Error resolving component '...'. It was detected that '...' has no namespace, but components with no target namespace are not referenceable from schema document 'null'. If '...' is intended to have a namespace, perhaps a prefix needs to be provided. If it is intended that '...' has no namespace, then an 'import' without a "namespace" attribute should be added to 'null'.``  
- When present remove springIbisTestTool[name].xml and add property ibistesttool.custom=[name] to DeploymentSpecifics.properties. The springIbisTestTool[name].xml should now be present in IAF jars, mail springIbisTestTool[name].xml to Jaco or Peter to double check



6.0
---

[Commits](https://github.com/ibissource/iaf/compare/v5.6.1...v6.0)
[![Build Status](https://travis-ci.org/ibissource/iaf.png?branch=v6.0)](https://travis-ci.org/ibissource/iaf)

- Add support for jetty-maven-plugin
- Add note "Theme Bootstrap is part of a beta version" in main page of IBIS console for theme "bootstrap"
- Put regular form fields received by rest calls in sessionKeys (next to file form fields) so they can be used in the pipeline
- Add xslt2 attribute to parameter for using XSLT 2.0 instead of only XSLT 1.0
- Avoid PipeRunException when moving a file to an already existing destination (by adding a counter suffix)
- Add possibility to log the length of messages instead of the content in the MSG log
- Add functionality to forward form fields as sessionKeys (in RestListeners)
- Add possibility to write log records (to separate log files) without the message itself (e.g. for making counts)
- Configuration warning when FxF directory doesn't exist
- Added parameter pattern 'uuid' (which can be used instead of the combination of 'hostname' and 'uid')
- Add preemptive authentication to prevent “httpstatus 407: Proxy Authentication Required" when proxy is used without an user in a http call
- Make the IBIS console function "Browse a Jdbc table" capable for MQ SQL (next to Oracle)
- Performance fix for the IBIS console function "Browse a Jdbc table" 
- Add queue info when getting queue messages (currently used in "ShowTibcoQueues" in IJA_TiBeT2)
- Add possibility to wait for a database row to be locked (instead of always skipping locked records)
- Add functionality to temporarily move and/or chomp received messages for memory purposes
- Remove error about maximum size (10 MB) exceeding messages and increase the similar warning size from 1 MB to 3 MB. For HTTP messages increase the warning size from 32 KB to 128 KB
- Add possibility to fix the namespace of TIBCO response messages (instead of just copying the namespace)
- Enable parameters in xpathExpression of XmlSwitch
- Fix bug in namespace awareness (which was introduced in November 2013)
- Performance fix for the IBIS console function "Browse a Jdbc table" (get column information directly instead of by selecting first record)
- Write loaded configuration to file when IBIS is started so it's possible to query on it (e.g. via Splunk)
- Fix bug "(SQLException) FOR UPDATE clause allowed only for DECLARE CURSOR" for non Oracle dbms which was introduced with lockWait attribute
- Fix strange bug in DirectoryListener (which occurred in Ibis4Scan)
- Bugfix database actions not being part of transaction when using BTM
- Add possibility for a RestListener to stream received documents from and into a database table
- Better m2e configuration (no need to overwrite/change org.eclipse.wst.common.component anymore)
- Add class to browse and remove queue messages with input and output a xml message (very useful for test purposes)
- Move Test Tool 1 from IbisTestToolWEB to Maven module Ibis AdapterFramework Larva
- Show http body in exception thrown by http sender in case status code indicates an error
- Make multipart work for http sender in case only inputMessageParam is used (without extra parameters)
- Bugfix RestListenerServlet that didn't read http body anymore for POST method
- Add support for paramsInUrl, inputMessageParam and multipart to Larva HttpSender
- Add custom pipe for interrupting processing when timeout is exceeded
- Add facility to show the age of the current first message in the queue when pendingMsgCount>0 and receiverCount=0 (currently used in "ShowTibcoQueues" in IJA_TiBeT2)
- Add facility to check EsbJmsListeners on lost connections
- Next to the methode type GET and POST, also the method types PUT and DELETE are now possible in HttpSender
- With the base64 attribute in HttpSender it is possible to receive and pass on non-string results
- Add completeFileHeader attribute in ZipWriterPipe
- Bugfix NPE when changing log level in console and nonstandard log4j configuration is used
- Use default log4j config in example webapp
- Use AppConstants in Larva Test Tool when testtool.properties not found
- Configure example webapp to use Larva Test Tool
- Add Test.properties to AppConstants for local properties which should not be added to the version control system
- Bugfix ForEachChildElementPipe with blockSize which was skipping remaining items after last block
- Remove datasource.deleteTable function from Larva
- Remove autoignore function from Larva
- Replace testtool.properties with AppConstants
- Remove unused and broken Larva jsp's
- Add scenariosroot.default property
- Add support to Larva for Maven based Eclipse projects
- Sort available scenarios root directories before unavailable ones
- Adjust the filling of the ESB Common Message Header in the SOAP Header
- Add copyAEProperties attribute in EsbJmsListener
- Add charset parameter to MailSender
- Make MailSender parameters messageType and messageBase64 thread-safe
- Add queueRegex attribute to GetTibcoQueues
- Add defaultValueMethods to Parameter
- Don't use ConversationId from previous sender response
- Add GetPrincipalPipe and IsUserInRolePipe to stub4testtool.xsl
- Add MessageStoreSender and MessageStoreListener
- Add size of message to response in GetTibcoQueues (and chomp very large message)
- Add writeSuffix parameter to FileHandler (next to writeSuffix attribute)
- Add file type bin for mime type application/octet-stream in FileViewerServlet
- Make it possible to generate a WSDL based on WsdlXmlValidator
- Add Last Message to Show ConfigurationStatus
- Bugfix monitoring events for input/output-Validators/Wrappers being ignored
- Add support for multiple versions of the ESB Common Message Header
- Add SOAP 1.2 support for WsdlXmlValidator and WSDL generator
- Add interval attribute to scheduler job (run job directly after all adapters are started)
- Continue moving files of directory listener file list after failure
- Add protocol attribute to HttpSender (to use TLS next to SSL)
- `(end of v6.0-RC1)`
- Add move message action to EsbJmsFFListeners in the main page of the IBIS console
- Add facility to provide public rest services
- Extend functionality of GetTibcoQueues (currently used in "ShowTibcoQueues" in IJA_TiBeT2)
- Remove useless space in SOAP envelop element
- Improve code to prevent double attributes in merged schema
- Add useCdataSection attribute to Text2XmlPipe
- Add getRowNumber method to dbms support classes
- Always show 'Reload configuration' link in 'Show configurationStatus'
- Fix menubar at top of page in IBIS console
- Add list of REST services next to list of WSDL's and renamed IBIS console function WSDL's to Webservices
- Avoid exception "RestListener for uriPattern [...] method [...] already configured" for ConfigurationServlet
- Add returnResults to ManageDatabase
- Update to latest Ladybug Test Tool version
    - Show error message for Rerun on Report level too
    - Show error message for Run in Test tab
- Adjust IBIS console classic theme to look more like bootstrap theme
- `(end of v6.0-RC2)`
- Add WebServiceListener to ManageDatabase to generate WSDL (on LOC, DEV and TST)
- Improve show configuration status page layout and add flow images
- Prevent "Premature end of file" in System.err on isWellFormed check
- Print adapter description on show configuration status page
- Improve HelloWorld(s) adapter
- Improve javadoc for soap attribute of WebServiceListener
- Bugfix losing message when OutOfMemoryError during resend message from ErrorLog
- Add functionality to easily create IBIS specific views
- Add view attribute to RestListener (to put a link in the IBIS console)
- Improve Larva txt diff performance (especially for large messages)
- Add fromClasspath to Larva XsltProviderListener
- Add facility to recover adapters
- Add facility to show pending message count for receiving queues in 'Show configurationStatus'
- Extend checkForDuplicates facility in ReceiverBase with correlationID (next to messageID)
- Make CleanupOldFilesPipe more flexible
- Add support for subdirectories to UnzipPipe
- Add facility to use wildcard in 'Adapter Logging'
- Add FxfListener with possibility to move file after being processed
- Larva indent function: Normalise spaces around attribute names and values
- Use log.debug instead of log.info for "is about to call pipe" (was/is only called when log.level is DEBUG because log.isDebugEnabled() is used)
- Add facility to hide strings in log records
- `(end of v6.0-RC3)`
- Add H2 database support
- Larva: Switch parameters for WinDiff to show colors as intended
- Don't check Errorlog for records when errorStore.count.show=false
- Add parameter type 'bytes' to QuerySenders
- Add TimeoutGuardSenderWithParametersBase for better timeout handling in senders
- Add support for FxF3 version 2
- Larva: add DelaySender
- Add class to scan and report TIBCO sources in Subversion
- Add facility to GetTibcoQueues to retrieve principal description directly from LDAP
- Add facility of streaming result from HttpSender to file
- Add methodTypes HEAD and REPORT to HttpSender
- Add facility of streaming a file to a RestListener
- Prevent problems with control characters in console
- Bugfix adapters not sorted in 'Test a PipeLine' after run
- Remove METT tracing functionality
- Add possibility to log user info to separate log file (*-SEC.log) about IBIS console usage
- Add authorization roles to RestListener
- Introduction of XmlBuilderPipe to convert special characters to their xml equivalents
- Larva: add possibility to compare binary files (via FileListener)
- Larva: Add functionality to easily replace fixed strings
- Change interval recover job from 15 minutes to 5 minutes
- `(end of v6.0-RC4)`
- Avoid NumberFormatException in log4j (which was introduced in September 2015 and caused by slf4j jar file)
- Ladybug: Hide the same data as is hidden in the Ibis logfiles based on the log.hideRegex property in log4j4ibis.properties
- Ladybug: Prefix html title with "Ladybug"
- Support multiple operations/listeners per service/adapter in generated WSDL
- Improve values of name attributes in generated WSDL which will make a WSDL easier to understand
- Use generic targetNamespace prefix (ibis->tns) in generated WSDL
- Only add jms namespace when needed in generated WSDL
- Ladybug: Prevent NPE (due to previous change) when Tibet2 specific Report is instantiated
- Avoid NPE in TextXmlPipe (which was introduced in April 2015)
- Fix broken flow images for adapters which contain space in name
- Don't show svg (flow) images in IE 9 and 10
- Log status of adapters and receivers to separate heartbeat log at regular intervals
- Show formatted file size in IBIS console function "Adapter logging"
- Only include modify functions for security log
- Replace job cleanupFxf by new job cleanupFileSystem to easily add other directories to cleanup
- Prevent IllegalStateException "ServerDataProvider already registered" when reconnecting SapListener
- Add log directory to job cleanupFileSystem (with 60 days retention and without subdirectories)
- Use lower cases for all files in the log directory
- `(end of v6.0-RC5)`
- Avoid NPE in ShowConfigurationStatus when queueConnectionFactory jmsRealm is not defined
- Bugfix XmlValidator recovers wrongly
- Remove obsolete/broken repositories from pom.xml
- Improve validation (also to support backward compatibility)
- Improve validation on root, soapBody and soapHeader
    - Add configuration warning when root not specified
    - Add configuration warning when soapBody not specified
    - Add configuration warning when root, soapBody and soapHeader not found in available XSD's
    - Don't allow any element when soapHeader not specified
    - Don't allow any element when soapBody not specified
    - Don't allow soapHeader or soapBody to have soap namespace (e.g. when element doesn't have an xmlns attribute)
- `(end of v6.0)`
- Prevent warnings about root elements which are actually available in imports and/or includes
- Add option to specify root elements as comma separated list of names to choose from (only one is allowed)
- Prevent warnings about root elements which are actually available in redefines


### Non backwards compatible changes

- The use of 'xsd:import' and 'xsd:include' in xsd files in XmlValidator (and subclasses) has become more strictly.
	- ~~``sch-props-correct.2: A schema cannot contain two global components with the same name; this schema contains two occurrences of 'http://nn/nl/XSD/Generic/MessageHeader/1, ...'.``  
	When using the EsbSoapValidator, don't import the CommonMessageHeader xsd in a main xsd but only import the namespace (because this xsd already exists within IAF). For using a deviating CommonMessageHeader xsd, use the SoapValidator.~~
	- ``src-resolve: Cannot resolve the name 'cmh:Result' to a(n) 'element declaration' component.``  
	For validating ESB SOAP messages use the EsbSoapValidator and not the XmlValidator.
	- ``Circural dependencies between schemas.``  
	Unused imported or included schemas can be ignored by using the validator attributes importedSchemaLocationsToIgnore and importedNamespacesToIgnore.
- The use of 'xsd:redefine' doesn't work for schemaLocation anymore (still works for schema). It's deprecated in the latest specification (http://www.w3.org/TR/xmlschema11-1/#modify-schema) and difficult to support in WSDL generation.
- (from RC5) From now all files in the log directory are in lower cases. This can affect applications which are case sensitive and use one or more files from the IBIS log directory.



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
- Support for FXF 1 and 2 has been dropped.



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