<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:param name="maxMemory" />
	<xsl:template match="/">
		<xsl:value-of select="$maxMemory" disable-output-escaping="yes"/>
	</xsl:template>
</xsl:stylesheet>