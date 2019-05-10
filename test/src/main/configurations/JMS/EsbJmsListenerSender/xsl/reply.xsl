<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" indent="yes"/>
	<xsl:template match="/">
		<reply>
			<root>
				<xsl:value-of select="local-name(*)"/>
			</root>
		</reply>
	</xsl:template>
</xsl:stylesheet>
