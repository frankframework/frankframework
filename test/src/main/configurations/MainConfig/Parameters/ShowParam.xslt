<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml"/>
	
	<xsl:param name="param1"/>
	<xsl:param name="param2"/>
	<xsl:param name="param3"/>
	
	<xsl:template match="/">
		<xsl:element name="root">
			<xsl:element name="param1"><xsl:value-of select="$param1"/></xsl:element>
			<xsl:element name="param2"><xsl:value-of select="$param2"/></xsl:element>
			<xsl:element name="param3"><xsl:value-of select="$param3"/></xsl:element>
		</xsl:element>
	</xsl:template>
</xsl:stylesheet>

