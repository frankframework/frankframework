<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml"/>
	
	<xsl:template match="/">
		<xsl:element name="Wrapper">
			<xsl:copy-of select="/"/>
		</xsl:element>
	</xsl:template>
</xsl:stylesheet>