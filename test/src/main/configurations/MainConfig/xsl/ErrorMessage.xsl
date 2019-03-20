<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<xsl:param name="errorCode" />
	<xsl:param name="failureReason" />
	<xsl:template match="/">
		<Error>
			<errorCode>
				<xsl:value-of select="$errorCode" />
			</errorCode>
			<errorMessage>
				<xsl:value-of select="$failureReason" />
			</errorMessage>
		</Error>
	</xsl:template>
</xsl:stylesheet>
