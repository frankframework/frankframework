<?xml version="1.0" encoding="UTF-8"?>
<!--
This stylesheet normalizes space in PCDATA and attributes in an XML file.

By changing the xsl:strip-space and xsl:preserve-space values, the script can be
changed to handle whitespace nodes (text nodes with all whitespace between successive
elements) by replacing them with a single space.

The stylesheet can be modified to ignore specified elements and attributes also,
for example when these should not be compared.

-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!-- No extra whitespace on output -->
	<xsl:output method="xml" indent="yes" encoding="UTF-8" omit-xml-declaration="yes"/>
	<xsl:strip-space elements="*"/>
	<!-- can add here any elements where whitspace nodes are important
		<xsl:preserve-space elements="xx yy"/>
	-->
	<xsl:template match="/">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="*">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>
	<!-- any elements that need to be ignored are matched here 
		To ignore an attribute, use @attrbute-to-ignore in the match string
		Always separate each item with a '|' symbol as shown -->
	<xsl:template match="element-to-ignore | another-element-to-ignore"/>
	<xsl:template match="*[@xml:space='preserve']">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:apply-templates mode="preserve-space"/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="*" mode="preserve-space">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:apply-templates mode="preserve-space"/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="text()" mode="preserve-space">
		<xsl:value-of select="."/>
	</xsl:template>
	<xsl:template match="text()">
		<xsl:value-of select="normalize-space()"/>
	</xsl:template>
	<!-- Normalize space on attributes -->
	<xsl:template match="@*">
		<xsl:variable name="attr-namespace" select="namespace-uri()"/>
		<xsl:choose>
			<xsl:when test="string-length($attr-namespace)>0">
				<xsl:attribute name="{local-name()}" namespace="{namespace-uri()}"><xsl:value-of select="normalize-space()"/></xsl:attribute>
			</xsl:when>
			<xsl:otherwise>
				<xsl:attribute name="{local-name()}"><xsl:value-of select="normalize-space()"/></xsl:attribute>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
