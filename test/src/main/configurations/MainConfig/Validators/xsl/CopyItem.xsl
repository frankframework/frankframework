<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="urn:items">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" />
	
	<xsl:template match="/" >
		<xsl:element name="Item_Response">
			<xsl:copy-of select="*/*"/>
			<Result xmlns="http://nn.nl/XSD/Generic/MessageHeader/1">
				<Status>OK</Status>
			</Result>
		</xsl:element>
	</xsl:template>
</xsl:stylesheet>