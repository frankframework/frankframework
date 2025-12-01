<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0" xmlns:xs="http://www.w3.org/2001/XMLSchema">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<!-- Parameter disableValidators has been used to test the impact of validators on memory usage -->
	<xsl:param name="disableValidators"/>

	<xsl:template match="/">
		<xsl:apply-templates select="*|comment()|processing-instruction()" />
	</xsl:template>

	<xsl:template match="*|@*|comment()|processing-instruction()">
		<xsl:call-template name="copy" />
	</xsl:template>

	<xsl:template match="adapter">
		<xsl:element name="connector">
			<xsl:apply-templates select="@*|node()" />
		</xsl:element>
	</xsl:template>

	<xsl:template name="copy">
		<xsl:copy>
			<xsl:apply-templates select="*|@*|comment()|processing-instruction()|text()" />
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>
