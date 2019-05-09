<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes" />

	<xsl:param name="attachment1" />
	<xsl:param name="attachment2" />

	<xsl:template match="/">
        <attachments>
            <first><xsl:value-of select="$attachment1"/></first>
            <second><xsl:value-of select="$attachment2"/></second>
        </attachments>
	</xsl:template>
</xsl:stylesheet>
