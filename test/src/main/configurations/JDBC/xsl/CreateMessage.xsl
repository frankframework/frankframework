<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<xsl:template match="/">
	<result>
		<xsl:apply-templates select="row/field"/>
	</result>
	</xsl:template>
	<xsl:template match="field">
		<xsl:choose>
			<xsl:when test="@name='TCHAR'">
			<message>
				<xsl:value-of select="." disable-output-escaping="yes"/>
			</message>
			</xsl:when>
			<xsl:when test="@name='TKEY'">
			<id>
				<xsl:value-of select="." ></xsl:value-of>
			</id>
			</xsl:when>
		</xsl:choose>
		
		
	</xsl:template>
	
</xsl:stylesheet>
