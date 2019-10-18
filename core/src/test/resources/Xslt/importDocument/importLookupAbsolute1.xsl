<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
	<xsl:variable name="lookup" select="document('/Xslt/importDocument/lookup.xml')"/>
	<xsl:template match="root">
		<start>
			<xsl:apply-templates select="names"/>
			<xsl:apply-templates select="firstnames"/>
		</start>
	</xsl:template>
	<xsl:template match="names">
		<Namen>
			<xsl:apply-templates select="name"/>
		</Namen>
	</xsl:template>
	<xsl:template match="firstnames">
		<Voornamen>
			<xsl:apply-templates select="name"/>
		</Voornamen>
	</xsl:template>
	<xsl:template match="name">
		<Naam>
			<xsl:variable name="name" select="."/>
			<xsl:variable name="color" select="$lookup/names/name[.=$name]/@color"/>
			<xsl:attribute name="kleur"><xsl:value-of select="$color"/></xsl:attribute>
			<xsl:value-of select="."/>
		</Naam>
	</xsl:template>
</xsl:stylesheet>
