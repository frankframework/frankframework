<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" />

	<xsl:template match="/">
		<root>
			<xsl:copy-of select="." />
			<test>
				<xsl:value-of select="//Id" />
			</test>
		</root>
	</xsl:template>

</xsl:stylesheet>
