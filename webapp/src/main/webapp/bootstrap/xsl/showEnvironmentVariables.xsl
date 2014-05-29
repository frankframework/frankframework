<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" doctype-system="about:legacy-compat" />
	<xsl:include href="functions.xsl"/>
	<xsl:include href="components.xsl"/>
	<xsl:template match="/">
		<html lang="nl-NL">
			<xsl:call-template name="header">
				<xsl:with-param name="css">showLogging.css</xsl:with-param>
				<xsl:with-param name="title"><xsl:value-of select="/page/applicationConstants/properties/property[@name='instance.name']"/> - Show Environment Variables</xsl:with-param>
			</xsl:call-template>
			<body>
				<xsl:call-template name="menu">
					<xsl:with-param name="environment"><xsl:value-of select="/page/applicationConstants/properties/property[@name='otap.stage']"/></xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="modal">
					<xsl:with-param name="version"><xsl:value-of select="/page/applicationConstants/properties/property[@name='instance.name']"/> ??? ????????-????, IAF <xsl:value-of select="/page/applicationConstants/properties/property[@name='application.version']"/>, buildscript ??, size: ??</xsl:with-param>
					<xsl:with-param name="server">running on ??? using ???</xsl:with-param>
					<xsl:with-param name="heap">heap size: ??M, total JVM memory: ??M</xsl:with-param>
					<xsl:with-param name="space">free space: ??GB, total space: ??GB</xsl:with-param>
				</xsl:call-template>
				<div class="panel panel-primary env-val">
					<div class="panel-heading">
						<h3 class="panel-title">Environment Variables</h3>
					</div>
					<div class="panel-body">
					<xsl:for-each select="page/attribute[@name='envVars']/environmentVariables/propertySet">
					
						<xsl:variable name="tableId"> 
							<xsl:value-of select="position()"/>
						</xsl:variable>
						
						
						<table class="table table-bordered">
							<caption><xsl:value-of select="@name"/></caption>
							<thead>
								<tr>
									<th class="table-header">ID</th>
									<th class="table-header">Property</th>
									<th class="table-header">Value</th>
								</tr>
							</thead>
							<tbody>
							<xsl:for-each select="property">							
							
								<tr>
									<td><xsl:value-of select="concat($tableId, '-', position())"/></td>
									<td><xsl:value-of select="@name"/></td>
									<td><xsl:value-of select="."/></td>
								</tr>							
							
							</xsl:for-each>	
							</tbody>				
						</table>
					
						
					</xsl:for-each>
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