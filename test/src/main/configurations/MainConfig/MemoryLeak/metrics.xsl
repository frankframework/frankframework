<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" >
<xsl:output method="text" omit-xml-declaration="yes" indent="no"/>
	<xsl:template match="/">
		<xsl:value-of select="round(number(substring(processMetrics/properties/property[@name='maxMemory'], 0, string-length(processMetrics/properties/property[@name='maxMemory']))) div 455) * 470000" />
	</xsl:template>
</xsl:stylesheet>