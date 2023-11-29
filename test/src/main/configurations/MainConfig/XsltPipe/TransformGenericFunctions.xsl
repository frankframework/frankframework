<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:frankext="http://frankframework.org/frankext"
	xmlns:xalan="http://xml.apache.org/xslt"
	exclude-result-prefixes="xalan frankext"
	version="1.0">

	<xsl:import href="genericFunctions.xsl" />

	<xsl:output method="text" />

	<xsl:template match="/">
		<xsl:value-of select="frankext:replaceCharsInString(//request/textIn)" />
	</xsl:template>

</xsl:stylesheet>
