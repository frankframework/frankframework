<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes" />

    <xsl:variable name="jmsRealm">
        <xsl:value-of select="manageDatabaseREQ/@jmsRealm"/>
    </xsl:variable>
    
    <xsl:template match="/manageDatabaseREQ/@jmsRealm" />
    
	<xsl:template match="/manageDatabaseREQ/sql | /manageDatabaseREQ/insert | /manageDatabaseREQ/select | /manageDatabaseREQ/update | /manageDatabaseREQ/delete | /manageDatabaseREQ/alter">
		<xsl:copy>
			<xsl:text disable-output-escaping="yes">
           		<xsl:attribute name="jmsRealm">
					<xsl:choose>
						<xsl:when test="./@jmsRealm != ''"><xsl:value-of select="./@jmsRealm"/></xsl:when>
						<xsl:otherwise><xsl:value-of select="$jmsRealm"/></xsl:otherwise>
					</xsl:choose>
            	</xsl:attribute>
            	<xsl:apply-templates select="@*|node()" />
         	</xsl:text>
       	</xsl:copy>
     </xsl:template>

	<xsl:template match="@*|node()">
		<xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
	</xsl:template>
</xsl:stylesheet>