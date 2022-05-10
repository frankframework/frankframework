<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="text" version="1.0" encoding="UTF-8" indent="yes" omit-xml-declaration="yes"/>

	<xsl:template match="/">
		<xsl:value-of select="name(/*[local-name()='Envelope']/*[local-name()='Body']/*[name()!='MessageHeader'])"/>
	</xsl:template>

</xsl:stylesheet>