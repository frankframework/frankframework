<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ibis="xalan://nl.nn.adapterframework.util.DateUtils" xmlns:log4j="http://jakarta.apache.org/log4" version="1.0">
	<xsl:output method="text" indent="no"/>
	<xsl:template match="log4j:log4j">
		<xsl:apply-templates select="log4j:event"/>
	</xsl:template>
	<xsl:template match="log4j:event">
		<xsl:variable name="timestamp">
			<xsl:value-of select="@timestamp"/>
		</xsl:variable>
		<xsl:value-of select="ibis:format($timestamp,'yyyy-MM-dd-HH:mm:ss')"/>
		<xsl:text>&#160;</xsl:text>
		<xsl:value-of select="@level"/>
		<xsl:if test="string-length(@level)=4">
			<xsl:value-of select="' '"/>
		</xsl:if>
		<xsl:text>&#160;</xsl:text>
		<xsl:value-of select="log4j:NDC"/>
		<xsl:text>&#160;</xsl:text>
		<xsl:value-of select="@thread"/>
		<xsl:text>&#160;</xsl:text>
		<xsl:value-of select="@logger"/>
		<xsl:text>&#160;</xsl:text>
		<xsl:value-of select="log4j:message"/>
		<xsl:if test="throwable">
			<xsl:text>&#10;</xsl:text>
			<xsl:value-of select="log4j:throwable"/>
		</xsl:if>
		<xsl:text>&#10;</xsl:text>
	</xsl:template>
</xsl:stylesheet>
