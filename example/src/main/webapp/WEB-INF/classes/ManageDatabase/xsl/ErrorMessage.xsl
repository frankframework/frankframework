<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<xsl:param name="errorCode" />
	<xsl:param name="failureReason" />
	<xsl:template match="/">
		<manageDatabaseRLY>
			<completionInformation>
				<returnCode>NOT_OK</returnCode>
				<reasonCode>
					<xsl:value-of select="$errorCode" />
				</reasonCode>
				<message>
					<xsl:value-of select="$failureReason" />
				</message>
			</completionInformation>
		</manageDatabaseRLY>
	</xsl:template>
</xsl:stylesheet>
