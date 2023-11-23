<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:ibisext="http://frankframework.org/ibisext"
	xmlns:xalan="http://xml.apache.org/xslt"
	exclude-result-prefixes="xalan ibisext"
	version="1.0">
	
	<xsl:import href="genericFunctions.xsl" />
	
	<xsl:output method="text" />

	<xsl:template match="/">
		<xsl:value-of select="ibisext:replaceCharsInString(//request/textIn)" />
	</xsl:template>

</xsl:stylesheet>
