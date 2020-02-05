<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:fn="http://www.w3.org/2005/xpath-functions" >
	<xsl:output method="xml" />
 <xsl:template match="/">
	 <xsl:copy-of select="/root/sub"/>
 </xsl:template>
</xsl:stylesheet>