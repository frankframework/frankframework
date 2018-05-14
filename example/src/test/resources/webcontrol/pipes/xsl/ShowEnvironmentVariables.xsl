<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>
	<xsl:template match="/">
		<xsl:apply-templates select="*|@*|comment()|processing-instruction()"/>
	</xsl:template>
	<xsl:template match="*">
		<xsl:choose>
			<xsl:when test="name()='property'">
				<!-- skip -->
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="copy"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="comment()|processing-instruction()|@*">
		<xsl:call-template name="copy"/>
	</xsl:template>
	<xsl:template name="copy">
		<xsl:copy>
			<xsl:apply-templates select="*|@*|comment()|processing-instruction()|text()"/>
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>
