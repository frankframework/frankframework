<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes"/>
	<xsl:template match="html">
		<xml>
			<xsl:apply-templates select="head"/>
			<xsl:apply-templates select="body"/>
		</xml>
	</xsl:template>
	<xsl:template match="head">
		<head>
			<title>
				<xsl:variable name="ibis" select="substring-before(title, '@')"/>
				<xsl:variable name="function" select="substring-after(title, ':')"/>
				<xsl:value-of select="concat($ibis,': ',$function)"/>
			</title>
		</head>
	</xsl:template>
	<xsl:template match="body">
		<body>
			<xsl:for-each select="table/tbody/tr">
				<tr>
					<xsl:choose>
						<xsl:when test="position()=1">
							<xsl:comment>Number of links in menu</xsl:comment>
							<tabBar>
								<xsl:value-of select="count(td/div/table/tbody/tr/td/a)"/>
							</tabBar>
						</xsl:when>
						<xsl:when test="position()=2">
							<h1>
								<xsl:value-of select="td/h1"/>
							</h1>
						</xsl:when>
						<xsl:when test="position()=3">
							<filler>
								<xsl:value-of select="count(td)"/>
							</filler>
						</xsl:when>
						<xsl:when test="position()=4">
							<xsl:for-each select="td/table">
								<caption>
									<xsl:value-of select="caption"/>
								</caption>
								<href>
									<xsl:choose>
										<xsl:when test="count(tbody/tr/td/a[@href])&gt;0">true</xsl:when>
										<xsl:otherwise>false</xsl:otherwise>
									</xsl:choose>
								</href>
							</xsl:for-each>
						</xsl:when>
						<xsl:otherwise>
							<xsl:copy-of select="*"/>
						</xsl:otherwise>
					</xsl:choose>
				</tr>
			</xsl:for-each>
		</body>
	</xsl:template>
</xsl:stylesheet>
