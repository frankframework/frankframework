<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />

	<xsl:param name="value" />
	<xsl:param name="sessionkey" />
	<xsl:param name="sessionkeyWithValue" />
	<xsl:param name="sessionkeyWithSessionKey" />

	<xsl:template match="/">
		<root>
			<input>
				<xsl:value-of select="local-name(*)"/>
			</input>
			<sessionKeys>
				<value><xsl:value-of select="$value" disable-output-escaping="yes"/></value>
				<sessionkey><xsl:value-of select="$sessionkey" disable-output-escaping="yes"/></sessionkey>
				<sessionkeyWithValue><xsl:value-of select="$sessionkeyWithValue" disable-output-escaping="yes"/></sessionkeyWithValue>
				<sessionkeyWithSessionKey><xsl:value-of select="$sessionkeyWithSessionKey" disable-output-escaping="yes"/></sessionkeyWithSessionKey>
			</sessionKeys>
		</root>
	</xsl:template>
</xsl:stylesheet>
