<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<xsl:param name="srcPrefix" />
	<xsl:template match="/">
		<page title="Webservices">
			<xsl:choose>
				<xsl:when test="name(*)='error'">
					<font color="red">
						<xsl:value-of select="error" />
					</font>
				</xsl:when>
				<xsl:otherwise>
					<table>
						<caption class="caption">Available REST services</caption>
						<xsl:for-each select="webservices/rests/rest">
							<tr>
								<td class="filterRow">
									<xsl:value-of select="position()" />
								</td>
								<td class="filterRow">
									<a>
										<xsl:attribute name="href">
									<xsl:value-of select="concat($srcPrefix,'rest/',@uriPattern)" />
								</xsl:attribute>
										<xsl:value-of select="@name" />
									</a>
								</td>
							</tr>
						</xsl:for-each>
					</table>
					<br />
					<br />
					<table>
						<caption class="caption">Available WSDL's</caption>
						<xsl:for-each select="webservices/wsdls/wsdl">
							<tr>
								<td class="filterRow">
									<xsl:value-of select="position()" />
								</td>
								<xsl:choose>
									<xsl:when test="error">
										<td class="filterRow">
											<xsl:value-of select="@name" />
										</td>
										<td class="filterRow">
											<xsl:value-of select="error" />
										</td>
									</xsl:when>
									<xsl:otherwise>
										<td class="filterRow">
											<a>
												<xsl:attribute name="href">
											<xsl:value-of
													select="concat($srcPrefix,'rest/webservices/',@name,@extention)" />
										</xsl:attribute>
												<xsl:value-of select="@name" />
											</a>
										</td>
										<td class="filterRow">
											<a>
												<xsl:attribute name="href">
											<xsl:value-of
													select="concat($srcPrefix,'rest/webservices/',@name,@extention,'?useIncludes=true')" />
										</xsl:attribute>
												<xsl:text>using includes</xsl:text>
											</a>
											<a>
												<xsl:attribute name="href">
											<xsl:value-of
													select="concat($srcPrefix,'rest/webservices/',@name,'.zip')" />
										</xsl:attribute>
												<xsl:text>zip</xsl:text>
											</a>
										</td>
									</xsl:otherwise>
								</xsl:choose>
							</tr>
						</xsl:for-each>
					</table>
				</xsl:otherwise>
			</xsl:choose>
		</page>
	</xsl:template>
</xsl:stylesheet>
