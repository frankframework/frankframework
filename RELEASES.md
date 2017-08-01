Ibis AdapterFramework release notes
===================================

[Tags](https://github.com/ibissource/iaf/releases)



Upcoming
--------

[JavaDocs](http://maven.ibissource.org/iaf/apidocs/index.html)
[Commits](https://github.com/ibissource/iaf/compare/v7.0-B1...HEAD)
[![Build Status](https://travis-ci.org/ibissource/iaf.png)](https://travis-ci.org/ibissource/iaf)

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