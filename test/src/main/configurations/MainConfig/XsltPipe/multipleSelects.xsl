<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes" />
	<xsl:template match="request">
		<reply>
			<count>
				<xsl:value-of select="count(id)" />
			</count>
			<id>
				<xsl:value-of select="id" />
			</id>
		</reply>
	</xsl:template>
</xsl:stylesheet>
