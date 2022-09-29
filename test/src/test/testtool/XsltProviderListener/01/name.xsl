<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

	<xsl:template match="root">
		<start>
			<xsl:apply-templates select="names"/>
		</start>
	</xsl:template>

	<xsl:template match="names">
		<namen>
			<xsl:apply-templates select="name"/>
		</namen>
	</xsl:template>

	<xsl:template match="name">
		<naam>
			<xsl:value-of select="."/>
		</naam>
	</xsl:template>

</xsl:stylesheet>
