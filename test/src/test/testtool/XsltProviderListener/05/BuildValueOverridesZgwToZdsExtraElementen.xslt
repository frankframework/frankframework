<xsl:stylesheet version="3.0" exclude-result-prefixes="#all" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:StUF="http://www.egem.nl/StUF/StUF0301">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" omit-xml-declaration="yes" />
	<xsl:strip-space elements="*"/>

	<xsl:mode on-no-match="shallow-skip" />

	<xsl:param name="PreValueOverridesContext" as="node()?"><xsl:document /></xsl:param>

	<xsl:param name="debug" as="xs:string" select="'false'" />
	<xsl:variable name="debugSerializeParams" as="map(xs:string, item()?)">
		<xsl:map>
			<xsl:map-entry key="'indent'" select="true()"/>
		</xsl:map>
	</xsl:variable>

	<xsl:template match="/">
		<StUF:extraElementen>
			<xsl:apply-templates />
		</StUF:extraElementen>
	</xsl:template>

	<xsl:template match="*[starts-with(text(), '${extraElementen:') and ends-with(text(), '}')]">
		<xsl:if test="$debug = 'true'"><xsl:comment expand-text="yes"> extraElementenSection: [{serialize(., $debugSerializeParams)}] </xsl:comment></xsl:if>

		<xsl:variable name="extraElement">
			<StUF:extraElement>
				<xsl:variable name="extraElementName" select="substring-after(substring-before(., '}'), ':')" />
				<xsl:attribute name="naam"><xsl:value-of select="$extraElementName"/></xsl:attribute>
				<xsl:if test="$debug = 'true'"><xsl:comment expand-text="yes"> extraElementName: [{serialize($extraElementName, $debugSerializeParams)}] </xsl:comment></xsl:if>

				<xsl:if test="$debug = 'true'"><xsl:comment expand-text="yes"> xpath: [{serialize(path(), $debugSerializeParams)}] </xsl:comment></xsl:if>
				<xsl:evaluate context-item="$PreValueOverridesContext" xpath="path() || '/text()'"></xsl:evaluate>
			</StUF:extraElement>
		</xsl:variable>

		<xsl:if test="$extraElement != ''">
			<xsl:copy-of select="$extraElement" />
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>
