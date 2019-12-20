<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="no" omit-xml-declaration="yes"/>
	<xsl:variable name="totalRecords">
		<xsl:value-of select="count(//*[local-name()='XDOC'])"/>
	</xsl:variable>
	
	<xsl:template match="/">
		<xsl:apply-templates select="*"/>
	</xsl:template>
	
<!-- Maakt 1 insert van alle aangeboden xdocs -->
	<xsl:template match="*">
		<xsl:for-each select="*[local-name()='XDOC']"><xsl:copy-of select="." /><xsl:choose>
				<xsl:when test="position() = $totalRecords"/>
				<xsl:otherwise>,</xsl:otherwise>
			</xsl:choose>
		</xsl:for-each>
	</xsl:template>

</xsl:stylesheet>
