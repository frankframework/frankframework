<?xml version="1.0" encoding="UTF-8"?>
<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XML-schema" version="2.0">
    <xsl:param name="message" />
    <xsl:param name="testMessage" />
    <xsl:template match="/">
        <result><xsl:value-of select="contains($message, $testMessage)" /></result>
    </xsl:template>
</xsl:transform>