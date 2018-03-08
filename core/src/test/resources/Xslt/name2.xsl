<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" />
	<!-- replaced following line to avoid error 'Failed to compile stylesheet' during Maven Test (locally it ran fine) --> 
	<!--xsl:variable name="lookup" select="document('colorLookup.xml')" /-->
	<xsl:variable name="lookup" select="document('lookup.xml')" />
	<xsl:template match="name">
		<Naam>
			<xsl:variable name="name" select="." />
			<xsl:attribute name="kleur" select="$lookup/names/name[.=$name]/@color"></xsl:attribute>
			<xsl:value-of select="." />
		</Naam>
	</xsl:template>
</xsl:stylesheet>
