<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:include href="functions.xsl"/>
	<xsl:include href="components.xsl"/>
	<xsl:template match="/">
		<html lang="nl-NL">
			<xsl:call-template name="header">
				<xsl:with-param name="css">showLogging.css</xsl:with-param>
				<xsl:with-param name="title"><xsl:value-of select="/page/applicationConstants/properties/property[@name='instance.name']"/> - Show Logging</xsl:with-param>
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
						<h3 class="panel-title">Logfiles</h3>
					</div>
					<div class="panel-body">
						<table class="table table-bordered table-striped">
							<tr>
								<th>Name</th>
								<th>Size</th>
								<th>Date</th>
								<th>Time</th>
								<th>as</th>
							</tr>
							<xsl:for-each select="page/attribute/directory/file">
								<tr>
									<td>
										<xsl:value-of select="@name"/>
									</td>
									<td>
										<xsl:value-of select="@size"/>
									</td>
									<td>
										<xsl:value-of select="@modificationDate"/>
									</td>
									<td>
										<xsl:value-of select="@modificationTime"/>
									</td>
									<td>
										<xsl:choose>
											<xsl:when test="@directory = 'true'">
												<a>
													<xsl:attribute name="href"><xsl:value-of select="concat('showLogging.do?directory', '=', @canonicalName)"/></xsl:attribute>
												directory</a>
											</xsl:when>
											<xsl:otherwise>
												<a class="log">
													<xsl:attribute name="href"><xsl:value-of select="concat('FileViewerServlet?resultType', '=', 'xml&amp;fileName', '=', @canonicalName)"/></xsl:attribute>
												xml</a>
												<a class="log">
													<xsl:attribute name="href"><xsl:value-of select="concat('FileViewerServlet?resultType', '=', 'text&amp;fileName', '=', @canonicalName)"/></xsl:attribute>
												plain</a>
												<xsl:choose>
													<xsl:when test="contains(@canonicalName, '_xml.log')">
														<a class="log">
															<xsl:attribute name="href"><xsl:value-of select="concat('FileViewerServlet?resultType', '=', 'html&amp;fileName', '=', @canonicalName)"/></xsl:attribute>2html</a>
														<a class="log">
															<xsl:attribute name="href"><xsl:value-of select="concat('FileViewerServlet?resultType', '=', 'text&amp;fileName', '=', @canonicalName, '&amp;log4j=true')"/></xsl:attribute>
												2text</a>
													</xsl:when>
													<xsl:when test="contains(@canonicalName, '-stats_')">
														<a class="log">
															<xsl:attribute name="href"><xsl:value-of select="concat('FileViewerServlet?resultType', '=', 'html&amp;fileName', '=', @canonicalName)"/></xsl:attribute>2html</a>
													</xsl:when>
												</xsl:choose>
											</xsl:otherwise>
										</xsl:choose>
									</td>
								</tr>
							</xsl:for-each>
						</table>
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
