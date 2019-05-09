<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="2.0">
	<xsl:output method="text" indent="no" />
	<xsl:param name="baseUrl" />
	<xsl:param name="docId" />
	<xsl:template match="/">
		<xsl:value-of select="concat($baseUrl,'/',$docId)" />
	</xsl:template>
</xsl:stylesheet>