<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"  xmlns:j="http://www.w3.org/2013/XSL/json">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
	
	<xsl:param name="header" select="'header'"/>
	
	<xsl:template match="/">
		<j:map>
			<j:string key="header"><xsl:value-of select="$header"/></j:string>
			<j:array key="data">
				<xsl:for-each select="//j:map[@key='employees']/j:map[not(j:string[@key='manager'])]">
					<xsl:call-template name="member" />
				</xsl:for-each>
			</j:array>
		</j:map>
	</xsl:template>
	
	<xsl:template name="member" >
		<xsl:variable name="member" select="."/>
		<j:map>
			<j:string key="key"><xsl:value-of select="@key"/></j:string>
			<j:string key="firstname"><xsl:value-of select="j:string[@key='firstname']"/></j:string>
			<j:string key="surname"><xsl:value-of select="j:string[@key='surname']"/></j:string>
			<xsl:variable name="teammembers" select="//j:map[@key='employees']/j:map[j:string[@key='manager']=$member/@key]"/>
			<xsl:if test="$teammembers">
				<j:array key="team">
					<xsl:for-each select="$teammembers">
						<xsl:call-template name="member" />
					</xsl:for-each>
				</j:array>
			</xsl:if>
		</j:map>	
	</xsl:template>
</xsl:stylesheet>
