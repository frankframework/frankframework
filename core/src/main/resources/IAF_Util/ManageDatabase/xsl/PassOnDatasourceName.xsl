<?xml version="1.0" encoding="UTF-8"?>
<!-- Transforms ManageDatabase input, copying the ManageDatabaseREQ's datasourceName attribute to all underlying <sql> tags that have no configured datasourceName attribute themselves. -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes" />

    <xsl:variable name="datasourceName">
        <xsl:value-of select="manageDatabaseREQ/@datasourceName"/>
    </xsl:variable>
    
    <xsl:template match="/manageDatabaseREQ/@datasourceName" />
    
	<xsl:template match="/manageDatabaseREQ/sql | /manageDatabaseREQ/insert | /manageDatabaseREQ/select | /manageDatabaseREQ/update | /manageDatabaseREQ/delete | /manageDatabaseREQ/alter">
		<xsl:copy>
			<xsl:if test="$datasourceName != ''">
           		<xsl:attribute name="datasourceName">
					<xsl:choose>
						<xsl:when test="./@datasourceName != ''"><xsl:value-of select="./@datasourceName"/></xsl:when>
						<xsl:otherwise><xsl:value-of select="$datasourceName"/></xsl:otherwise>
					</xsl:choose>
            	</xsl:attribute>
            </xsl:if>
           	<xsl:apply-templates select="@*|node()" />
       	</xsl:copy>
     </xsl:template>

	<xsl:template match="@*|node()">
		<xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
	</xsl:template>
</xsl:stylesheet>