<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:import href="name.xsl"/>
	<xsl:output method="xml" indent="yes"/>
	<xsl:template match="names">
		<Namen>
			<xsl:apply-templates select="name"/>
		</Namen>
	</xsl:template>
</xsl:stylesheet>
