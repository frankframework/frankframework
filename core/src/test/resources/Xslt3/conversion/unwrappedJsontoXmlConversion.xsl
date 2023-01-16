<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xs="http://www.w3.org/2001/XMLSchema">
	<xsl:output indent="yes" omit-xml-declaration="yes" method="xml" />

	<xsl:template match="/" name="xsl:initial-template">
		<xsl:copy-of select="json-to-xml($json)/*"/>
	</xsl:template>
	
	<xsl:param name="json" as="xs:string" expand-text="no">
{
	"content": [
		{
			"id": 70805774,
			"value": "1001",
			"position": [
				1004.0,
				288.0,
				1050.0,
				324.0
			]
		}
	]
}
	</xsl:param>

</xsl:stylesheet>