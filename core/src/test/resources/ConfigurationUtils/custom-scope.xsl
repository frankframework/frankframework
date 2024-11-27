<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:stub="http://frankframework.org/stub">
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
		<xsl:element name="adapter">
			<xsl:apply-templates select="@*" />
			<stub/>
		</xsl:element>
	</xsl:template>

	<xsl:template name="copy">
		<xsl:copy>
			<xsl:apply-templates select="*|@*|comment()|processing-instruction()|text()" />
		</xsl:copy>
	</xsl:template>

	<!-- Escape xml tag opening(<) and closing(>) signs so that xsl:comment can process the copy of the xml. Processes elements and attributes only -->
	<xsl:template match="*" mode="escape">
		<xsl:variable name="apos">'</xsl:variable>
		<!-- Start element -->
		<xsl:text>&lt;</xsl:text>
		<xsl:value-of select="name()" />

		<!-- Attributes -->
		<xsl:for-each select="@*">
			<xsl:value-of select="concat(' ', name(), '=', $apos, ., $apos)"/>
		</xsl:for-each>

		<!-- End opening tag -->
		<xsl:text>&gt;</xsl:text>

		<!-- Children -->
		<xsl:apply-templates select="node()" mode="escape" />

		<!-- End element -->
		<xsl:text>&lt;/</xsl:text>
		<xsl:value-of select="name()" />
		<xsl:text>&gt;</xsl:text>
	</xsl:template>
</xsl:stylesheet>
