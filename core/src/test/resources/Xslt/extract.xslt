<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0" >
<xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>

<xsl:template match="/">
	<xsl:value-of select="/result/field" disable-output-escaping="yes"/>
</xsl:template>

</xsl:stylesheet>