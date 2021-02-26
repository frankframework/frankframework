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
						<xsl:choose>
							<xsl:when test="count(webservices/rests/rest)=0">
								<tr>
									<td class="filterRow">No rest listeners found</td>
								</tr>
							</xsl:when>
							<xsl:otherwise>
								<xsl:for-each select="webservices/rests/rest">
									<xsl:sort select="@name" />
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
							</xsl:otherwise>
						</xsl:choose>
					</table>
					<br />
					<br />
					<table>
						<caption class="caption">Available WSDL's</caption>
						<xsl:choose>
							<xsl:when test="count(webservices/wsdls/wsdl)=0">
								<tr>
									<td class="filterRow">No registered listeners found</td>
								</tr>
							</xsl:when>
							<xsl:otherwise>
								<xsl:for-each select="webservices/wsdls/wsdl">
									<xsl:sort select="@name" />
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
															select="concat($srcPrefix,'rest/webservices/',@name,@extension)" />
										</xsl:attribute>
														<xsl:value-of select="@name" />
													</a>
												</td>
												<td class="filterRow">
													<a>
														<xsl:attribute name="href">
											<xsl:value-of
															select="concat($srcPrefix,'rest/webservices/',@name,@extension,'?useIncludes=true')" />
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
							</xsl:otherwise>
						</xsl:choose>
					</table>
					<br />
					<br />
					<table>
						<caption class="caption">Available API services</caption>
						<xsl:choose>
							<xsl:when test="count(webservices/apiListeners/apiListener)=0">
								<tr>
									<td class="filterRow">No ApiListeners found</td>
								</tr>
							</xsl:when>
							<xsl:otherwise>
								<thead>
									<tr>
										<td class="filterRow">#</td>
										<td class="filterRow">endpoint</td>
										<td class="filterRow">GET</td>
										<td class="filterRow">POST</td>
										<td class="filterRow">PUT</td>
										<td class="filterRow">DELETE</td>
									</tr>
								</thead>
								<tbody>
									<xsl:for-each select="webservices/apiListeners/apiListener">
										<tr>
											<td class="filterRow">
												<xsl:value-of select="position()" />
											</td>
											<td class="filterRow">
												<xsl:value-of select="@uriPattern" />
											</td>
											<td class="filterRow">
												<xsl:if test="count(GET) &gt; 0">
													<xsl:value-of select="GET/@name" />
													<xsl:text></xsl:text>
												</xsl:if>
											</td>
											<td class="filterRow">
												<xsl:if test="count(POST) &gt; 0">
													<xsl:value-of select="POST/@name" />
												</xsl:if>
											</td>
											<td class="filterRow">
												<xsl:if test="count(PUT) &gt; 0">
													<xsl:value-of select="PUT/@name" />
												</xsl:if>
											</td>
											<td class="filterRow">
												<xsl:if test="count(DELETE) &gt; 0">
													<xsl:value-of select="DELETE/@name" />
												</xsl:if>
											</td>
										</tr>
									</xsl:for-each>
								</tbody>
							</xsl:otherwise>
						</xsl:choose>
					</table>
				</xsl:otherwise>
			</xsl:choose>
		</page>
	</xsl:template>
</xsl:stylesheet>
