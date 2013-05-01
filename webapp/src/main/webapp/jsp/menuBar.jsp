<%@ page import="nl.nn.adapterframework.util.AppConstants"%>

<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<menuBar>
	<imagelinkMenu>
			<imagelink 
				href="showConfigurationStatus.do"
				type="configurationStatus"
				alt="show Configuration Status">
			</imagelink>
			<imagelink 
				href="showConfiguration.do"
				type="configuration"
				alt="show Configuration">
			</imagelink>
			<imagelink 
				href="showLogging.do"
				type="logging"
				alt="show Logging">
			</imagelink>
			<imagelink 
				href="sendJmsMessage.do"
				type="jms-message"
				alt="send a message with JMS">
			</imagelink>
			<imagelink 
				href="testIfsaService.do"
				type="ifsa-message"
				alt="call an IFSA Service">
			</imagelink>
			<imagelink 
				href="browseQueue.do"
				type="browsejms"
				alt="browse a queue with JMS">
			</imagelink>
			<imagelink
				href="testPipeLine.do"
				type="testPipeLine"
				alt="test a PipeLine of an Adapter">
			</imagelink>
			<imagelink
				href="testService.do"
				type="service"
				alt="test a ServiceListener">
			</imagelink>
			<imagelink
				href="servlet/rpcrouter"
				type="wsdl"
				alt="retrieve WSDL">
			</imagelink>
			<imagelink
				href="showSchedulerStatus.do"
				type="scheduler"
				alt="show Scheduler status">
			</imagelink>
			<imagelink
				href="showEnvironmentVariables.do"
				type="properties"
				alt="show Environment Variables">
			</imagelink>
			<imagelink
				href="executeJdbcQuery.do"
				type="execquery"
				alt="execute a Jdbc Query">
			</imagelink>
			<imagelink
				href="browseJdbcTable.do"
				type="browsetable"
				alt="browse a Jdbc Table">
			</imagelink>
			<imagelink
				href="DumpIbisConsole"
				type="dump"
				alt="dump Ibis Console">
			</imagelink>
			<imagelink 
				href="showTracingConfiguration.do"
				type="tracingConfiguration"
				alt="show Tracing Configuration">
			 </imagelink>
			<imagelink
				href="showSecurityItems.do"
				type="security"
				alt="show Security Items">
			</imagelink>
			<imagelink 
				href="showMonitors.do"
				type="monitoring"
				alt="show Monitors">
			</imagelink>
			<imagelink 
				href="showIbisstoreSummary.do"
				type="showsummary"
				alt="show Ibisstore Summary">
			</imagelink>
			<imagelink
				href="testtool"
				type="testtool"
				alt="testTool">
			</imagelink>
			<imagelink
				href="<%= AppConstants.getInstance().getResolvedProperty("help.url") %>"
				type="help"
				newwindow="true">
			</imagelink>
	</imagelinkMenu>
</menuBar>