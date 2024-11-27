<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:param name="parNode" />
	<xsl:template match="/">
		<root>
			<xsl:for-each select="$parNode">
				<xsl:value-of select="name(.)" />
				<xsl:text>, </xsl:text>
			</xsl:for-each>
		</root>
	</xsl:template>
</xsl:stylesheet>
