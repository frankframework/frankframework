<?xml version="1.0"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output omit-xml-declaration="yes" method="text" />
	<xsl:template match="data">
		<xsl:variable name="input">
			<xsl:copy>
				<xsl:apply-templates select="json-to-xml(.)/*"/>
			</xsl:copy>
		</xsl:variable>
		<xsl:value-of select="name($input/*[local-name()='data']/*[local-name()='Envelope']/*[local-name()='Body']/*[name()!='MessageHeader'])"/>
	</xsl:template>
	<xsl:template match="*[@key]">
		<xsl:element name="{@key}">
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
</xsl:stylesheet>