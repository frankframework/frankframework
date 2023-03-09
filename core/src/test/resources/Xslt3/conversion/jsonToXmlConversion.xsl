<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xs="http://www.w3.org/2001/XMLSchema">
	<xsl:output indent="yes" omit-xml-declaration="yes" method="xml" />

	<xsl:template match="/">
	    <xsl:copy-of select="json-to-xml(.)"/>
	</xsl:template>

</xsl:stylesheet>