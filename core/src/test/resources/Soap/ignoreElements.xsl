<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" omit-xml-declaration="yes"/>
	
	<xsl:template match="*|@*|comment()|processing-instruction()">
		<xsl:copy>
			<xsl:apply-templates select="*|@*|comment()|processing-instruction()|text()"/>
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="*[local-name()='Security']">
		<xsl:element name="{name()}" namespace="{namespace-uri()}">
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()"/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="*[local-name()='Signature']">
		<xsl:element name="{name()}" namespace="{namespace-uri()}">
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()"/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="*[local-name()='Reference']">
		<xsl:element name="{name()}" namespace="{namespace-uri()}">
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()"/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="*[local-name()='Body']">
		<xsl:element name="{name()}" namespace="{namespace-uri()}">
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()"/>
		</xsl:element>
	</xsl:template>
	
	
	<xsl:template match="*[local-name()='Timestamp']">REPLACED</xsl:template>
	<xsl:template match="*[local-name()='Password']">REPLACED</xsl:template>
	<xsl:template match="*[local-name()='Nonce']">REPLACED</xsl:template>
	<xsl:template match="*[local-name()='Created']">REPLACED</xsl:template>
	<xsl:template match="*[local-name()='KeyInfo']">REPLACED</xsl:template>
	<xsl:template match="*[local-name()='SignatureValue']">REPLACED</xsl:template>
	<xsl:template match="*[local-name()='DigestValue']">REPLACED</xsl:template>
</xsl:stylesheet>