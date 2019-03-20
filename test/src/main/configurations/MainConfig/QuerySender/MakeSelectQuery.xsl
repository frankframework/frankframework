<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="text" indent="no"/>
	<xsl:template match="request">
		<xsl:text>SELECT * FROM IBISPROP WHERE VALUE = </xsl:text>
		<xsl:text>&apos;</xsl:text>
		<xsl:value-of select="value"/>
		<xsl:text>&apos;</xsl:text>
	</xsl:template>
</xsl:stylesheet>
