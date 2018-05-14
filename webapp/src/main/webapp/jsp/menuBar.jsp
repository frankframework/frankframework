<%@ page import="nl.nn.adapterframework.monitoring.MonitorManager"%>
<%@ page import="nl.nn.adapterframework.util.AppConstants"%>
<%@ page import="nl.nn.adapterframework.util.XmlUtils"%>

<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<menuBar>
	<imagelinkMenu>
			<imagelink 
				href="rest/showConfigurationStatus"
				type="configurationStatus"
				alt="Show Configuration Status">
			</imagelink>
			<imagelink 
				href="rest/showConfiguration"
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

			<% if (AppConstants.getInstance().getBoolean("active.ifsa", false)) { %>
				<imagelink 
					href="rest/testIfsaService"
					type="ifsa-message"
					alt="Call an IFSA Service">
				</imagelink>
			<% } %>

			<imagelink 
				href="browseQueue.do"
				type="browsejms"
				alt="Browse a queue with JMS">
			</imagelink>
			<imagelink
				href="rest/testPipeLine"
				type="testPipeLine"
				alt="Test a PipeLine of an Adapter">
			</imagelink>
			<imagelink
				href="testService.do"
				type="service"
				alt="Test a ServiceListener">
			</imagelink>
			<imagelink
				href="rest/webservices"
				type="wsdl"
				alt="Webservices">
			</imagelink>
			<imagelink
				href="showSchedulerStatus.do"
				type="scheduler"
				alt="Show Scheduler status">
			</imagelink>
			<imagelink
				href="rest/showEnvironmentVariables"
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
				href="showSecurityItems.do"
				type="security"
				alt="Show Security Items">
			</imagelink>
			<% if (MonitorManager.getInstance().isEnabled()) { %>
				<imagelink 
					href="showMonitors.do"
					type="monitoring"
					alt="Show Monitors">
				</imagelink>
			<% } %>
			<imagelink 
				href="showIbisstoreSummary.do"
				type="showsummary"
				alt="Show Ibisstore Summary">
			</imagelink>
			<imagelink
				href="larva"
				type="larva"
				alt="Larva Test Tool">
			</imagelink>
			<imagelink
				href="testtool"
				type="ladybug"
				alt="Ladybug Test Tool">
			</imagelink>
			<imagelink
				href="javascript:void(0)"
				type="info"
				alt="Information">
			</imagelink>

			<% if (AppConstants.getInstance().getBoolean("console.active", false)) { %>
				<imagelink
					href="iaf/gui"
					type="theme"
					alt="Try our new GUI 3.0 and leave feedback!"> 
				</imagelink>
			<% } %>
	</imagelinkMenu>
</menuBar>