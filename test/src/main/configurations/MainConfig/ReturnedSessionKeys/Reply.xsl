<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<xsl:param name="returnCode" />
	<xsl:param name="reasonCode" />
	<xsl:param name="dataStream" />
	<xsl:param name="localValue" />
	<xsl:param name="exitState" />
	<xsl:param name="forward" />
	<xsl:template match="/">
		<reply>
			<returnCode>
				<xsl:value-of select="$returnCode" />
			</returnCode>
			<reasonCode>
				<xsl:value-of select="$reasonCode" />
			</reasonCode>
			<dataStream>
				<xsl:value-of select="$dataStream" />
			</dataStream>
			<localValue>
				<xsl:value-of select="$localValue" />
			</localValue>
			<pipelineResult>
				<xsl:value-of select="." />
			</pipelineResult>
			<exitState>
				<xsl:value-of select="$exitState" />
			</exitState>
			<forward>
				<xsl:value-of select="$forward" />
			</forward>
		</reply>
	</xsl:template>
</xsl:stylesheet>
