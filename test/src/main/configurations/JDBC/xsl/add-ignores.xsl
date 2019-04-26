<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:fn="http://www.w3.org/2005/xpath-functions">

	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>

    <xsl:param name="tnumber"/>

	<xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="*/rowset/row/field[@name='TNUMBER']">
        <xsl:copy>
			<xsl:apply-templates select="@*" />
			<xsl:value-of select="replace(current(), $tnumber, 'IGNORE')"/>
        </xsl:copy>
   </xsl:template>
</xsl:stylesheet>