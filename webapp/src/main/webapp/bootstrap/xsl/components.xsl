<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<!-- HEADER -->
	<xsl:template name="header">
		<xsl:param name="css"/>
		<xsl:param name="title"/>
		<head>
			<meta charset="utf-8"/>
			<title>
				<xsl:value-of select="$title"/>
			</title>
			<meta name="viewport" content="initial-scale=1,width=device-width"/>
			<meta http-equiv="X-UA-Compatible" content="IE=edge"/>
			<link href="bootstrap/css/flatly.bootstrap.css" rel="stylesheet" media="all"/>
			<link href="bootstrap/css/style.css" rel="stylesheet" media="all"/>
			<link rel="stylesheet" media="all">
				<xsl:attribute name="href"><xsl:value-of select="concat('bootstrap/css/', $css)"/></xsl:attribute>
			</link>						
			
  			<xsl:text disable-output-escaping="yes">
  				&lt;!--[if lt IE 9]&gt;
  					&lt;script src="bootstrap/js/html5shiv-3.6.2.min.js"&gt;&lt;/script&gt;
  					&lt;script src="bootstrap/js/respond.1.3.0.min.js"&gt;&lt;/script&gt;
  				&lt;![endif]--&gt;
  			</xsl:text>
			
		</head>
	</xsl:template>
	<!-- MENU -->
	<xsl:template name="menu">
		<xsl:param name="environment"/>
		<div class="navbar navbar-default navbar-fixed-top navbar-grey">
			<div class="navbar-collapse collapse navbar-responsive-collapse"> <!-- class="navbar-collapse collapse navbar-responsive-collapse"-->
				<ul class="nav navbar-nav">
					<li>
						<a href="showConfigurationStatus.do" title="Show Configuration Status">
							<img src="bootstrap/img/configurationStatus.gif" alt="Show Configuration Status"/>
						</a>
					</li>
					<li>
						<a href="showConfiguration.do" title="Show Configuration">
							<img src="bootstrap/img/configuration.gif" alt="Show Configuration"/>
						</a>
					</li>
					<li>
						<a href="showWsdls.do" title="WSDL's">
							<img src="bootstrap/img/wsdl.gif" alt="WSDL's"/>
						</a>
					</li>
					<li>
						<a href="testPipeLine.do" title="Test a PipeLine of an Adapter">
							<img src="bootstrap/img/testPipeLine.gif" alt="Test a PipeLine of an Adapter"/>
						</a>
					</li>
					<li>
						<a href="testService.do" title="Test a ServiceListener">
							<img src="bootstrap/img/service.gif" alt="Test a ServiceListener"/>
						</a>
					</li>
					<li>
						<a href="sendJmsMessage.do" title="Send a message with JMS">
							<img src="bootstrap/img/jmsmessage.gif" alt="Send a message with JMS"/>
						</a>
					</li>
					<li>
						<a href="executeJdbcQuery.do" title="Execute a Jdbc Query">
							<img src="bootstrap/img/execquery.gif" alt="Execute a Jdbc Query"/>
						</a>
					</li>
					<li>
						<a href="testTool.do" title="Test Tool">
							<img src="bootstrap/img/testtool.gif" alt="Test Tool"/>
						</a>
					</li>
					<li>
						<a href="browseQueue.do" title="Browse a queue with JMS">
							<img src="bootstrap/img/browsejms.gif" alt="Browse a queue with JMS"/>
						</a>
					</li>
					<li>
						<a href="showSchedulerStatus.do" title="Show Scheduler status">
							<img src="bootstrap/img/clock.gif" alt="Show Scheduler status"/>
						</a>
					</li>
					<li>
						<a href="showEnvironmentVariables.do" title="Show Environment Variables">
							<img src="bootstrap/img/config.gif" alt="Show Environment Variables"/>
						</a>
					</li>
					<li>
						<a href="browseJdbcTable.do" title="Browse a Jdbc Table">
							<img src="bootstrap/img/table.gif" alt="Browse a Jdbc Table"/>
						</a>
					</li>
					<li>
						<a href="showSecurityItems.do" title="Show Security Items">
							<img src="bootstrap/img/security.gif" alt="Show Security Items"/>
						</a>
					</li>
					<li>
						<a href="showMonitors.do" title="Show Monitors">
							<img src="bootstrap/img/monitoring.gif" alt="Show Monitors"/>
						</a>
					</li>
					<li>
						<a href="showIbisstoreSummary.do" title="Show Ibisstore Summary">
							<img src="bootstrap/img/ibisstore.gif" alt="Show Ibisstore Summary"/>
						</a>
					</li>
					<li>						
						<a href="showLogging.do" title="Show Logging">
							<img src="bootstrap/img/logging.gif" alt="Show Logging"/>
						</a>
					</li>
					<li>
						<a href="DumpIbisConsole" title="Dump Ibis Console">
							<img src="bootstrap/img/dump.gif" alt="Dump Ibis Console"/>
						</a>
					</li>
					<li>
						<a href="consoleGuide.do" title="ConsoleGuide">
							<img src="bootstrap/img/help.gif" alt="ConsoleGuide"/>
						</a>
					</li>
					<li>
						<a href="#" data-toggle="modal" data-target="#myModal" title="Information">
							<img src="bootstrap/img/info.gif" alt="Information"/>
						</a>
					</li>
					<li>
						<xsl:element name="a">
							<xsl:attribute name="href"><xsl:value-of select="/page/attribute[@name='nl.nn.adapterframework.webcontrol.ThemeSwitchQueryString']"/></xsl:attribute>
							<xsl:attribute name="title">Theme Classic</xsl:attribute>
							<img src="bootstrap/img/theme-switch.png" alt="Theme Classic"/>
						</xsl:element>
					</li>
				</ul>
				<ul class="nav navbar-nav navbar-right">
					<li>
						<span class="label label-warning label-environment" title="Theme Bootstrap is part of a beta version">beta</span>
					</li>
					<li>
						<xsl:variable name="class">
							<xsl:choose>
								<xsl:when test="$environment = 'LOC'">success</xsl:when>
								<xsl:when test="$environment = 'STUB'">warning</xsl:when>
								<xsl:when test="$environment = 'DEV'">info</xsl:when>
								<xsl:when test="$environment = 'TST'">info</xsl:when>
								<xsl:when test="$environment = 'ACC'">info</xsl:when>
								<xsl:when test="$environment = 'PRD'">danger</xsl:when>
							</xsl:choose>
						</xsl:variable>
						<xsl:variable name="label">
							<xsl:choose>
								<xsl:when test="$environment = 'LOC'">Local</xsl:when>
								<xsl:when test="$environment = 'STUB'">Stubbed</xsl:when>
								<xsl:when test="$environment = 'DEV'">Development</xsl:when>
								<xsl:when test="$environment = 'TST'">Test</xsl:when>
								<xsl:when test="$environment = 'ACC'">Acceptance</xsl:when>
								<xsl:when test="$environment = 'PRD'">Production</xsl:when>
							</xsl:choose>
						</xsl:variable>
						<span>
							<xsl:attribute name="class"><xsl:value-of select="concat('label', ' label-', $class, ' label-environment')"/></xsl:attribute>
							<xsl:value-of select="$label"/>
						</span>
					</li>
				</ul>
			</div>
		</div>
	</xsl:template>
	<!-- MODAL -->
	<xsl:template name="modal">
		<xsl:param name="version"/>
		<xsl:param name="server"/>
		<xsl:param name="heap"/>
		<xsl:param name="space"/>
		<!-- Modal -->
		<div class="modal fade" id="myModal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<h4 class="modal-title" id="myModalLabel">Information</h4>
					</div>
					<div class="modal-body">
						<xsl:value-of select="$version"/>
						<br/>
						<xsl:value-of select="$server"/>
						<br/>
						<xsl:value-of select="$heap"/>
						<br/>
						<xsl:value-of select="$space"/>
						<div id="clock"/>
						<div class="github">
							<img src="bootstrap/img/github.png" alt="GitHub"/>
							<a href="https://github.com/ibissource/iaf">https://github.com/ibissource/iaf</a>
						</div>
					</div>
					<div class="modal-footer">
						<button type="button" class="btn btn-success" data-dismiss="modal">Close</button>
					</div>
				</div>
			</div>
		</div>
	</xsl:template>
	<!-- FOOTER -->
	<xsl:template name="footer">
		<script src="bootstrap/js/jquery-1.10.2.min.js"/>
		<script src="bootstrap/js/bootstrap.min.js"/>
		<script src="bootstrap/js/functions.js"/>
	</xsl:template>
</xsl:stylesheet>
