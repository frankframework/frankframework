<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" doctype-system="about:legacy-compat" />
	<xsl:include href="functions.xsl"/>
	<xsl:include href="components.xsl"/>
	<xsl:template match="/">
		<html lang="nl-NL">
			<xsl:call-template name="header">
				<xsl:with-param name="css">showLogging.css</xsl:with-param>
				<xsl:with-param name="title"><xsl:value-of select="/page/applicationConstants/properties/property[@name='instance.name']"/> - Show Security Items</xsl:with-param>
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
				<div class="panel panel-primary">
					<div class="panel-heading">
						<h3 class="panel-title">Show Security Items</h3>
					</div>
					<div class="panel-body">
						<xsl:for-each select="page/attribute[@name='secItems']/securityItems">
						<table class="table table-bordered">
							<caption>Security Role Bindings</caption>
							<thead>
								<tr>
									<th class="table-header">Role</th>
									<th class="table-header">SpecialSubjects</th>
									<th class="table-header">Groups</th>
								</tr>
							</thead>
							<xsl:for-each select="securityRoleBindings/ApplicationBinding/authorizationTable/authorizations">
							<tbody>
								<xsl:variable name="count" select="count(groups)"/>
								<xsl:variable name="role" select="substring-after(role/@href,'#')"/>
								<xsl:variable name="srole" select="ancestor::*/applicationDeploymentDescriptor/application/security-role[@id=$role]/role-name"/>
								<tr>
									<td rowspan=""><xsl:value-of select="$srole"/>1</td>
									
									<td rowspan=""><xsl:value-of select="specialSubjects/@name"/></td>
									<td rowspan=""><xsl:value-of select="groups[1]/@name"/></td>
								</tr>
								<xsl-value-of select="remove(groups[1])"/>
								<xsl:for-each select="groups">
									<tr>
										<td><xsl:value-of select="@name"/></td>
									</tr>
								</xsl:for-each>		
							</tbody>
							</xsl:for-each>
						</table>
						
						
						
						
						<table class="table table-bordered">
							<caption>Used JmsRealms</caption>
							<tbody>
								<tr>
									<th class="table-header">Name</th>
									<th class="table-header">Datasource</th>
									<th class="table-header">QueueConnectionFactory</th>
									<th class="table-header">TopicConnectionFactory</th>
									<th class="table-header">Info</th>
								</tr>
								<xsl:for-each select="jmsRealms/jmsRealm">
									<tr ref="spannedRow">
										<xsl:variable name="count" select="count(info)"/>
										<td rowspan=""><xsl:value-of select="@name"/></td>
										<td rowspan=""><xsl:value-of select="@datasourceName"/></td>
										<td rowspan=""><xsl:value-of select="@queueConnectionFactoryName"/></td>
										<td rowspan=""><xsl:value-of select="@topicConnectionFactoryName"/></td>
										<td rowspan=""><xsl:value-of select="info[1]"/></td>
									</tr>
										<xsl:for-each select="info[position()>1]">
											<tr>
												<td>
													<xsl:value-of select="."/>
												</td>
											</tr>
										</xsl:for-each>
								</xsl:for-each>
							</tbody>
						</table>
						<table class="table table-bordered">
							<caption>Used SapSystems</caption>
							<tbody>
								<tr>
									<th class="table-header">Name</th>
									<th class="table-header">Info</th>
								</tr>
								<xsl:for-each select="sapSystems/sapSystem">
									<tr ref="spannedRow">
										<td><xsl:value-of select="@name"/></td>
										<td><xsl:value-of select="info"/></td>
									</tr>
								</xsl:for-each>
							</tbody>
						</table>
						<table class="table table-bordered">
						<caption>Used Authentication Entries</caption>
							<tbody>
								<tr>
									<th class="table-header">Alias</th>
									<th class="table-header">Username</th>
									<th class="table-header">Password</th>
								</tr>
								<xsl:for-each select="authEntries/entry">
									<tr ref="spannedRow">
										<td><xsl:value-of select="@alias"/></td>
										<td><xsl:value-of select="@userName"/></td>
										<td><xsl:value-of select="@passWord"/></td>
									</tr>
								</xsl:for-each>
							</tbody>
						</table>
						<table class="table table-bordered">
						<caption>Used Certificates</caption>
						<tbody>
							<tr>
								<th class="table-header">Adapter</th>
								<th class="table-header">Pipe</th>
								<th class="table-header">Certificate</th>
								<th class="table-header">Info</th>
							</tr>
							<xsl:for-each select="registeredAdapters/adapter/pipes/pipe/certificate">
								<xsl:sort select="@url"/>
								<xsl:variable name="url" select="@url"/>
								<xsl:choose>
									<xsl:when test="preceding::certificate[@url=$url]=true()">
										<tr>
											<td><xsl:value-of select="../../../@name"/></td>
											<td><xsl:value-of select="../@name"/></td>
											<td><xsl:value-of select="@name"/></td>
											<td>same certificate as above</td>
										</tr>
									</xsl:when>
									<xsl:otherwise>
										<xsl:variable name="count" select="count(info)+1"/>
										<tr>
											<td><xsl:value-of select="../../../@name"/></td>
											<td><xsl:value-of select="../@name"/></td>
											<td><xsl:value-of select="@name"/></td>
											<td><xsl:value-of select="@url"/></td>
										</tr>
										<xsl:for-each select="info">
											<tr>
												<td><xsl:value-of select="."/></td>
											</tr>
										</xsl:for-each>
									</xsl:otherwise>
								</xsl:choose>
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