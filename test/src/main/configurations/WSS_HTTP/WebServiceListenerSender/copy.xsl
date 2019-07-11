<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:param name="root" />
	<xsl:output method="xml" indent="yes" />
	<xsl:template match="/">
		<xsl:element name="{$root}">
			<xsl:apply-templates select="*|@*|comment()|processing-instruction()" />
		</xsl:element>
	</xsl:template>
	<xsl:template match="*|@*|comment()|processing-instruction()">
		<xsl:copy>
			<xsl:apply-templates select="*|@*|comment()|processing-instruction()|text()" />
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>
