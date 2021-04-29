<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" omit-xml-declaration="yes"/>
	
	<xsl:template match="*|@*|comment()|processing-instruction()">
		<xsl:copy>
			<xsl:apply-templates select="*|@*|comment()|processing-instruction()|text()"/>
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="*:Security">
		<xsl:element name="{name()}" namespace="{namespace-uri()}">
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()"/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="*:Signature">
		<xsl:element name="{name()}" namespace="{namespace-uri()}">
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()"/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="*:Reference">
		<xsl:element name="{name()}" namespace="{namespace-uri()}">
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()"/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="*:Body">
		<xsl:element name="{name()}" namespace="{namespace-uri()}">
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()"/>
		</xsl:element>
	</xsl:template>
	
	
	<xsl:template match="*:Timestamp">REPLACED</xsl:template>
	<xsl:template match="*:Password">REPLACED</xsl:template>
	<xsl:template match="*:Nonce">REPLACED</xsl:template>
	<xsl:template match="*:Created">REPLACED</xsl:template>
	<xsl:template match="*:KeyInfo">REPLACED</xsl:template>
	<xsl:template match="*:SignatureValue">REPLACED</xsl:template>
	<xsl:template match="*:DigestValue">REPLACED</xsl:template>
</xsl:stylesheet>