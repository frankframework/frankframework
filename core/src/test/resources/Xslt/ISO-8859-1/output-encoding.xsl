<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="no" omit-xml-declaration="yes" encoding="ISO-8859-1" />


	<xsl:template match="/">
		<xsl:text disable-output-escaping="yes">&lt;?xml version="1.0" encoding="ISO-8859-1"?&gt;</xsl:text>

		<xsl:copy-of select="." />
	</xsl:template>

</xsl:stylesheet>