<?xml version="1.0"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output omit-xml-declaration="yes" method="xml" />
	<xsl:template match="data">
			<xsl:copy>
				<xsl:apply-templates select="json-to-xml(.)/*"/>
			</xsl:copy>
	</xsl:template>
	<xsl:template match="*[@key]">
		<xsl:element name="{@key}">
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
</xsl:stylesheet>