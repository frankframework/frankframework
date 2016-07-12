<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<xsl:param name="srcPrefix" />
	<xsl:variable name="brvbar" select="'&#166;'" />
	<xsl:template match="/">
		<page title="Show Configurations">
			<xsl:if test="name(*)='error'">
				<font color="red">
					<xsl:value-of select="*" />
				</font>
			</xsl:if>
			<script type="text/javascript">
				<xsl:text disable-output-escaping="yes">
						//&lt;![CDATA[

						function changeBg(obj,isOver) {
							var color1="#8D0022";
							var color2="#b4e2ff";
							if (isOver) {
							    obj.style.backgroundColor=color1;
							    obj.style.color=color2;
							} else {
							    obj.style.backgroundColor=color2;
							    obj.style.color=color1;
							}
						}

						//]]&gt;
					</xsl:text>
			</script>

			<xsl:for-each select="configs/config">
				<xsl:variable name="jr" select="@jmsRealm" />
				<xsl:if test="position()&gt;1">
					<br />
				</xsl:if>
				<table>
					<caption class="caption">
						<xsl:value-of select="concat('Configuration (jmsRealm=', $jr, ')')" />
					</caption>
					<tr>
						<th class="colHeader">Actions</th>
						<th class="colHeader">Name</th>
						<th class="colHeader">Version</th>
						<th class="colHeader">Filename</th>
						<th class="colHeader">Creation timestamp</th>
						<th class="colHeader">User</th>
						<th class="colHeader">Config length</th>
					</tr>
					<xsl:for-each select="result/rowset/row">
						<xsl:variable name="class">
							<xsl:choose>
								<xsl:when test="position() mod 2 = 0">
									<text>rowEven</text>
								</xsl:when>
								<xsl:otherwise>
									<text>filterRow</text>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:variable>
						<tr>
							<xsl:attribute name="class">
								<xsl:value-of select="$class" />
							</xsl:attribute>
							<td class="filterRow">
								<imagelink type="showastext" newwindow="true">
									<parameter name="jmsRealm">
										<xsl:value-of select="$jr" />
									</parameter>
									<parameter name="name">
										<xsl:value-of select="field[@name='NAME']" />
									</parameter>
								</imagelink>
							</td>
							<td class="filterRow">
								<xsl:value-of select="field[@name='NAME']" />
							</td>
							<td class="filterRow">
								<xsl:value-of select="field[@name='VERSION']" />
							</td>
							<td class="filterRow">
								<xsl:value-of select="field[@name='FILENAME']" />
							</td>
							<td class="filterRow">
								<xsl:value-of select="field[@name='CRE_TYDST']" />
							</td>
							<td class="filterRow">
								<xsl:value-of select="field[@name='RUSER']" />
							</td>
							<td class="filterRow">
								<xsl:value-of select="field[@name='LEN_CONFIG']" />
							</td>
						</tr>
					</xsl:for-each>
				</table>
			</xsl:for-each>
		</page>
	</xsl:template>
</xsl:stylesheet>
