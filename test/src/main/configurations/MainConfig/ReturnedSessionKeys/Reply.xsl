<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<xsl:param name="returnCode" />
	<xsl:param name="reasonCode" />
	<xsl:param name="forward" />
	<xsl:template match="/">
		<reply>
			<returnCode>
				<xsl:value-of select="$returnCode" />
			</returnCode>
			<reasonCode>
				<xsl:value-of select="$reasonCode" />
			</reasonCode>
			<forward>
				<xsl:value-of select="$forward" />
			</forward>
		</reply>
	</xsl:template>
</xsl:stylesheet>
