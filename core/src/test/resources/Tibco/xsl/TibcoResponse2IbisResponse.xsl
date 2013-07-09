<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes"/>
	<xsl:param name="tibcoResponseNamespace"/>
	<xsl:param name="tibcoResponseSubNamespace"/>
	<xsl:param name="ibisResponseNamespace"/>

	<xsl:template match="/">
		<xsl:apply-templates select="*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='Response' and namespace-uri()=$tibcoResponseNamespace]/*[position()=1 and namespace-uri()=$tibcoResponseSubNamespace]"/>
	</xsl:template>

	<xsl:template match="*[namespace-uri()=$tibcoResponseSubNamespace]">
		<xsl:element name="{name()}" namespace="{$ibisResponseNamespace}">
			<xsl:copy-of select="@*"/>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>

	<xsl:template match="node()|@*">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="comment()|processing-instruction()">
		<xsl:copy>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>

</xsl:stylesheet>		
