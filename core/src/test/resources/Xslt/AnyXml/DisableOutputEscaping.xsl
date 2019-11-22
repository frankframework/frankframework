<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes" />
	<xsl:template match="/">
		<xsl:element name="envelope">begin
<xsl:text disable-output-escaping="yes">&lt;disabled-elements attr="value"/&gt;</xsl:text><other>&lt;enabled-elements attr="value"/&gt;</other>
			<xsl:text disable-output-escaping="yes">&lt;disabled-elements attr="value"/&gt;</xsl:text><xsl:element name="other">&lt;enabled-elements attr="value"/&gt;
</xsl:element>end
</xsl:element>
	</xsl:template>
</xsl:stylesheet>
