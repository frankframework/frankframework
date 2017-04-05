<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
	
	<xsl:template match="/">
		<feedbackform>
			<recipients>
				<recipient><xsl:text>1234@integrationpartners.nl</xsl:text></recipient>
			</recipients>
			<from>
				<xsl:choose>
					<xsl:when test="root/name=''"><xsl:text>Anonymous</xsl:text></xsl:when>
					<xsl:otherwise><xsl:value-of select="root/name" /></xsl:otherwise>
				</xsl:choose>
			</from>
			<subject><xsl:text>Feedback GUI 3.0</xsl:text></subject>
			<name>
				<xsl:choose>
					<xsl:when test="root/name=''"><xsl:text>Anonymous</xsl:text></xsl:when>
					<xsl:otherwise><xsl:value-of select="root/name" /></xsl:otherwise>
				</xsl:choose>
			</name>
			<message>
				<rating><xsl:value-of select="root/rating"/></rating>
				<feedback><xsl:value-of select="root/feedback" /></feedback>
			</message>
		</feedbackform>
	</xsl:template>
	
</xsl:stylesheet>