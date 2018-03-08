<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:import href="names.xsl"/>
	<xsl:import href="name.xsl"/>
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
	<xsl:template match="root">
		<start>
			<xsl:apply-templates select="names"/>
			<xsl:apply-templates select="firstnames"/>
		</start>
	</xsl:template>
	<xsl:template match="firstnames">
		<Voornamen>
			<xsl:apply-templates select="name"/>
		</Voornamen>
	</xsl:template>
</xsl:stylesheet>
