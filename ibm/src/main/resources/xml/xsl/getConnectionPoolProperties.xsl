<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="text" indent="no"/>
	<xsl:param name="providerType"/>
	<xsl:param name="jndiName"/>
	<xsl:template match="/">
		<xsl:variable name="provider" select="concat($providerType,'Provider')"/>
		<xsl:for-each select="XMI/*[name()=$provider]/factories[@jndiName=$jndiName]/connectionPool">
			<xsl:if test="(position()=1)=false()">
				<xsl:text>, </xsl:text>
			</xsl:if>
			<xsl:text>connectionPoolProperties={</xsl:text>
				<xsl:for-each select="@*[(name()='id')=false()]">
					<xsl:if test="(position()=1)=false()">
						<xsl:text>, </xsl:text>
					</xsl:if>
					<xsl:value-of select="name()"/>
					<xsl:text>=</xsl:text>
					<xsl:value-of select="."/>
				</xsl:for-each>
			<xsl:text>}</xsl:text>
		</xsl:for-each>
	</xsl:template>
</xsl:stylesheet>
