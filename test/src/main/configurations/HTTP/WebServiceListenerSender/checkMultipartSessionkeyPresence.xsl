<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" omit-xml-declaration="yes"/>

	<xsl:param name="multipart1"/>

    <xsl:template match="/">
        <xsl:choose>
            <xsl:when test="$multipart1 = 'NoMultiPart1'"><case>noFiles</case></xsl:when>
            <xsl:otherwise><case>oneFile</case></xsl:otherwise>
        </xsl:choose>
	</xsl:template>

</xsl:stylesheet>