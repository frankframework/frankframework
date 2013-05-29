<?xml version="1.0" encoding="UTF-8"?>
<!-- ErrorMessageFormatter, omitting the details section of the standard errorMessageFormat. -->
<!-- author: Johan Verrips -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<xsl:output indent="yes" media-type="text/xml" omit-xml-declaration="yes"/>

	<xsl:template match="/">
			<xsl:apply-templates/>
	</xsl:template>

	<!-- omit the details section -->
	<xsl:template match="details"/>

	<xsl:template match="*|@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()"/>
		</xsl:copy>
	</xsl:template>
	

</xsl:stylesheet>
