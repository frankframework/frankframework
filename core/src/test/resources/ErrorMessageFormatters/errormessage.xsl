<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
	<xsl:param name="exitCode"/>

	<xsl:template match="/">
		<result>
			<xsl:call-template name="errors">
				<xsl:with-param name="id" select="'1'"/>
				<xsl:with-param name="exitCD" select="$exitCode"/>
			</xsl:call-template>
		</result>
	</xsl:template>
	
	<xsl:template name="errors">
		<xsl:param name="id"/>
		<xsl:param name="exitCD"/>
		
		<error id="{$id}">
			<exitCode><xsl:value-of select="$exitCD"/></exitCode>
		</error>
	</xsl:template>
</xsl:stylesheet>
