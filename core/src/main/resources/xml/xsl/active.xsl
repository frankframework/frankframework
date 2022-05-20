<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<!--
		Copy all nodes exept:
		- elements with an active attribute that differs from true or !false (not case sensitive)
		- active attributes
	-->
	<xsl:template match="/">
		<xsl:apply-templates select="*|comment()|processing-instruction()" />
	</xsl:template>
	
	<xsl:template match="*[@active]">
		<xsl:variable name="activeLC" select="lower-case(@active)" />
		<xsl:if test="$activeLC='true' or $activeLC='!false'">
			<xsl:next-match/>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="@active"/>
	
	<xsl:template match="*|@*|comment()|processing-instruction()">
		<xsl:copy>
			<xsl:apply-templates select="*|@*|comment()|processing-instruction()|text()" />
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>