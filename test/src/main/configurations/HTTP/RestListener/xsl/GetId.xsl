<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="text" indent="no" />
	<xsl:param name="uri" />
	<xsl:template match="/">
		<xsl:value-of select="substring-after($uri,'/doc/')" />
	</xsl:template>
</xsl:stylesheet>