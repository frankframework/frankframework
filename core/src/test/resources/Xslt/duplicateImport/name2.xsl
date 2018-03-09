<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" />
	<xsl:template match="name">
		<Naam>
			<xsl:value-of select="." />
		</Naam>
	</xsl:template>
</xsl:stylesheet>
