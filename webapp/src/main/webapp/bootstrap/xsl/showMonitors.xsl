<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" doctype-system="about:legacy-compat" />
	<xsl:include href="functions.xsl"/>
	<xsl:include href="components.xsl"/>
	<xsl:template match="/">
		<html lang="nl-NL">
			<xsl:call-template name="header">
				<xsl:with-param name="css">showLogging.css</xsl:with-param>
				<xsl:with-param name="title">IJA_IPLnL 186 20131029-1413 - Show Monitors</xsl:with-param>
			</xsl:call-template>
			<body>
				<xsl:call-template name="menu">
					<xsl:with-param name="environment">TST</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="modal">
					<xsl:with-param name="version">IJA_IPLnL 186 20131029-1413, IAF 5.0-a27.3, buildscript 11g, size: 5.1</xsl:with-param>
					<xsl:with-param name="server">running on LPAB00000001894 using Apache Tomcat/7.0.22</xsl:with-param>
					<xsl:with-param name="heap">heap size: 89M, total JVM memory: 150M</xsl:with-param>
					<xsl:with-param name="space">free space: 68GB, total space: 74GB</xsl:with-param>
				</xsl:call-template>
				<div class="panel panel-primary">
					<div class="panel-heading">
						<h3 class="panel-title">Show Monitors</h3>
					</div>
					<div class="panel-body">
						<div class="list-group">
							<a
								class="list-group-item"
								href="showEvents.do"
								type="showEvents"
								text="Show all Events"
								>
								<parameter name="action">Show all Events</parameter>
							</a>
							<a
								class="list-group-item"
								href="showMonitorExecute.do"
								type="add"
								alt="createMonitor"
								text="Create a New Monitor"
								>
								<parameter name="action">Create a New Monitor</parameter>
							</a>
							<a
								class="list-group-item"
								href="showMonitorExecute.do"
								type="export"
								alt="exportConfig"
								text="Export Monitor Configuration"
								>
								<parameter name="action">Export Monitor Configuration</parameter>
							</a>
							<a
								class="list-group-item"
								href="showMonitors.do"
								type="status"
								alt="Show Status XML"
								>
								<parameter name="action">Show Status XML</parameter>
							</a>
						</div>
						<form action="/showMonitorExecute.do" enctype="multipart/form-data">
							<input type="hidden" property="action" value="edit"/>
							New Configuration File: <input type="file" property="configFile" title="Browse for ConfigFile"/>
							<br/>
							<br/>
							<table class="table table-bordered">
								<caption>Monitors</caption>
								<tbody>
									<tr>
										<th colspan="4">Monitors</th>
										<th colspan="5">State</th>
										<th colspan="7">Triggers</th>
									</tr>
									<tr>
										<th>Action</th>
										<th>Notifications</th>
										<th>Name</th>
										<th>Type</th>
										
										<th>Raised</th>
										<th>Changed</th>
										<th>Hits</th>
										<th>Source</th>
										<th>Severity</th>
											
										<th>Action</th>
										<th>Type</th>
										<th>EventCodes</th>
										<th>Sources</th>
										<th>Severity</th>
										<th>Threshold</th>
										<th>Period</th>
									</tr>
									<tr>
										<td>
											<a title="delete job">												
												<xsl:attribute name="href"><xsl:value-of select="concat('showMonitorExecute.do?action=deleteMonitor&amp;index=',position()-1)"/></xsl:attribute>
												<img src="bootstrap/img/delete.gif" alt="delete monitor"/>
											
											</a>
											<a title="edit job">												
												<xsl:attribute name="href"><xsl:value-of select="concat('editMonitor.do?action=edit&amp;index=',position()-1)"/></xsl:attribute>
												
												<img src="bootstrap/img/edit.gif" alt="edit monitor"/>
											</a>											
										</td>
										<td></td>
										
									</tr>
					
					 
				 				</tbody>
							</table>							
						</form>
					</div>
				</div>
				<script>
					var sd = "2013-11-07 10:02:27.320";
				</script>
				<xsl:call-template name="footer"/>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>