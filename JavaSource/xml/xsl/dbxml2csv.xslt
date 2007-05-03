<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="text" version="1.0" encoding="UTF-8"  />
	
	<xsl:template match="/result">
		<xsl:for-each select="fielddefinition/field"><xsl:if test="position()>1">,</xsl:if>"<xsl:value-of select="@name"/>"</xsl:for-each>
		<xsl:for-each select="rowset/row">
"<xsl:for-each select="field"><xsl:if test="position()>1">","</xsl:if><xsl:value-of select="."/></xsl:for-each>"</xsl:for-each>
	</xsl:template>

</xsl:stylesheet>
