<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="text" omit-xml-declaration="yes" indent="no"/>
	<xsl:param name="startTime" />
	<xsl:param name="endTime" />
	<xsl:template match="/">
		<xsl:value-of select="(number($endTime) - number($startTime))" disable-output-escaping="yes"/>
	</xsl:template>
</xsl:stylesheet>