<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>
	<xsl:template match="/">
		<DocPDF>
			<xsl:value-of select="'{sessionKey:ref_FileContent}'" disable-output-escaping="yes"/>
		</DocPDF>
	</xsl:template>
</xsl:stylesheet>
