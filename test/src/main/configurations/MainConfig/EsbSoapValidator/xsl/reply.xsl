<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:param name="returncode"/>
    <xsl:param name="reasoncode"/>
	<xsl:template match="/">
		<ServiceResponse>
			<Body>
				<returnCode> <xsl:value-of select="$returncode"/> </returnCode>
				<reasonCode> <xsl:value-of select="$reasoncode"/> </reasonCode>
			</Body>
		</ServiceResponse>
	</xsl:template>
</xsl:stylesheet>