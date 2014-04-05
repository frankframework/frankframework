<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template name="lowercase">
	    <xsl:param name="string"/>
	    <xsl:value-of select="translate($string, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ*', 'abcdefghijklmnopqrstuvwxyz')"/>
	</xsl:template>
</xsl:stylesheet>