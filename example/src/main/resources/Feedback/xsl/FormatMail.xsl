<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
	
	<xsl:template match="/">
		<feedbackform>
			<recipients>
				<recipient><xsl:text>1234@integrationpartners.nl</xsl:text></recipient>
			</recipients>
			<from><xsl:text>noreply@integrationpartners.nl</xsl:text></from>
			<subject><xsl:text>Feedback GUI 3.0</xsl:text></subject>
			<message>
				<xsl:text>Name: </xsl:text>
					<xsl:choose>
						<xsl:when test="root/name=''"><xsl:text>Anonymous</xsl:text><xsl:text>,&#xA;</xsl:text></xsl:when>
						<xsl:otherwise><xsl:value-of select="root/name" /><xsl:text>,&#xA;</xsl:text></xsl:otherwise>
					</xsl:choose>
				<xsl:text>Rating: </xsl:text><xsl:value-of select="root/rating"/><xsl:text>,&#xA;</xsl:text>
				<xsl:text>Feedback: </xsl:text><xsl:value-of select="root/feedback" /><xsl:text>&#xA;</xsl:text>
			</message>
		</feedbackform>
	</xsl:template>
	
</xsl:stylesheet>