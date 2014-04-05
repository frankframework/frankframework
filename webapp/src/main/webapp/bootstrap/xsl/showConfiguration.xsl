<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" doctype-system="about:legacy-compat" />
	<xsl:include href="functions.xsl"/>
	<xsl:include href="components.xsl"/>
	<xsl:template match="/">	
		<html lang="nl-NL">
			<xsl:call-template name="header">
				<xsl:with-param name="css">showLogging.css</xsl:with-param>
				<xsl:with-param name="title">IJA_IPLnL 186 20131029-1413 - Show Configuration</xsl:with-param>
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
						<h3 class="panel-title">Show Configuration</h3>
					</div>
					<div class="panel-body">
					
						<ul id="myTab" class="nav nav-tabs">
							<!-- xsl:attribute name="class">
								<xsl:value-of select="@class"/>
							</xsl:attribute-->
							<li>
								<a>							
									<xsl:attribute name="href">
										<xsl:value-of select="concat('configHandler.do?action', '=', 'showloadedconfig')"/>
									</xsl:attribute>
									<xsl:attribute name="onclick">
										<xsl:value-of select="@class" />	
									</xsl:attribute>
									Show original configuration	
								</a>
							</li>
							<li>
								<a>							
									<xsl:attribute name="href">
										<xsl:value-of select="concat('configHandler.do?action', '=', 'showoriginalconfig')"/>
									</xsl:attribute>
									<xsl:attribute name="onclick">
										<xsl:value-of select="@class" />	
									</xsl:attribute>
									Show loaded configuration
								</a>
								<!-- a data-toggle="tab" href="configHandler.do?action=showoriginalconfig">Show loaded configuration </a-->
								<!-- a>									
									<xsl:attribute name="href"><xsl:value-of select="concat('configHandler.do?action', '=', 'showoriginalconfig')"/></xsl:attribute>
									<xsl:attribute name="onclick">
									Show loaded configuration
									</xsl:attribute>
								</a-->
								<!-- a data-toggle="tab">									
									<xsl:attribute name="href"><xsl:value-of select="concat('configHandler.do?action', '=', 'showoriginalconfig')"/></xsl:attribute>
									Show loaded configuration
								</a-->
							</li>
						</ul>
					
						<pre>
							<xsl:value-of select="page/attribute[@name='configXML']"/>
						</pre>
					</div>
				</div>
				<form method="post" name="ConfigurationPropertiesForm" action="logHandler.do">
					<table class="table table-bordered">
						<caption>Dynamic parameters</caption>
						<tbody>		
							<tr>
								<td>Root Log level</td>
								<td>
									<select property="logLevel" >	
										<option value="DEBUG">DEBUG</option>
										<option value="INFO">INFO</option>
										<option value="WARN">WARN</option>
										<option value="ERROR">ERROR</option>
									</select> 	
								</td>
							</tr>
							<!-- todo: appconstants lezen en default zetten. -->
							  <tr>
								<td>Log intermediary results</td>
								<td>
									<input type="checkbox" property="logIntermediaryResults"/>
								</td>
							  </tr>
							  <tr>
								<td>Length log records</td>
								<td>
									<input type="text" property="lengthLogRecords" size="8" maxlength="16"/>
								</td>
							  </tr>		
							  <tr>
								<td>
								  <input type="reset" value="reset"/>
								</td>
								<td>
								  <input type="submit" value="send"/>
								</td>
							  </tr>			
						</tbody>
					</table>
				</form>
				<script>
					var sd = "2013-11-07 10:02:27.320";
				</script>
				<xsl:call-template name="footer"/>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>
