<%@ page import="nl.nn.adapterframework.util.AppConstants"%>

<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<menuBar>
	<imagelinkMenu>
			<imagelink 
				href="showConfigurationStatus.do"
				type="configurationStatus"
				alt="Show Configuration Status">
			</imagelink>
			<imagelink 
				href="showConfiguration.do"
				type="configuration"
				alt="Show Configuration">
			</imagelink>
			<imagelink 
				href="showLogging.do"
				type="logging"
				alt="Show Logging">
			</imagelink>
			<imagelink 
				href="sendJmsMessage.do"
				type="jms-message"
				alt="Send a message with JMS">
			</imagelink>
			<imagelink 
				href="testIfsaService.do"
				type="ifsa-message"
				alt="Call an IFSA Service">
			</imagelink>
			<imagelink 
				href="browseQueue.do"
				type="browsejms"
				alt="Browse a queue with JMS">
			</imagelink>
			<imagelink
				href="testPipeLine.do"
				type="testPipeLine"
				alt="Test a PipeLine of an Adapter">
			</imagelink>
			<imagelink
				href="testService.do"
				type="service"
				alt="Test a ServiceListener">
			</imagelink>
			<imagelink
				href="servlet/rpcrouter"
				type="wsdl"
				alt="WSDL's">
			</imagelink>
			<imagelink
				href="showSchedulerStatus.do"
				type="scheduler"
				alt="Show Scheduler status">
			</imagelink>
			<imagelink
				href="showEnvironmentVariables.do"
				type="properties"
				alt="Show Environment Variables">
			</imagelink>
			<imagelink
				href="executeJdbcQuery.do"
				type="execquery"
				alt="Execute a Jdbc Query">
			</imagelink>
			<imagelink
				href="browseJdbcTable.do"
				type="browsetable"
				alt="Browse a Jdbc Table">
			</imagelink>
			<imagelink
				href="DumpIbisConsole"
				type="dump"
				alt="Dump Ibis Console">
			</imagelink>
			<imagelink 
				href="showTracingConfiguration.do"
				type="tracingConfiguration"
				alt="Show Tracing Configuration">
			 </imagelink>
			<imagelink
				href="showSecurityItems.do"
				type="security"
				alt="Show Security Items">
			</imagelink>
			<imagelink 
				href="showMonitors.do"
				type="monitoring"
				alt="Show Monitors">
			</imagelink>
			<imagelink 
				href="showIbisstoreSummary.do"
				type="showsummary"
				alt="Show Ibisstore Summary">
			</imagelink>
			<imagelink
				href="testtool"
				type="testtool"
				alt="Test Tool">
			</imagelink>
			<imagelink
				href="<%= AppConstants.getInstance().getResolvedProperty("help.url") %>"
				type="help"
				alt="Help"
				newwindow="true">
			</imagelink>
			<imagelink
				href="<%=XmlUtils.encodeChars(themeSwitchQueryString)%>"
				type="theme"
				alt="Theme Bootstrap">
			</imagelink>
	</imagelinkMenu>
</menuBar>