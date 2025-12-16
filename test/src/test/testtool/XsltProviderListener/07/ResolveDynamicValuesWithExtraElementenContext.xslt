<xsl:stylesheet version="3.0" exclude-result-prefixes="#all" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:map="http://www.w3.org/2005/xpath-functions/map" xmlns:array="http://www.w3.org/2005/xpath-functions/array">
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" omit-xml-declaration="yes" />
    <xsl:strip-space elements="*"/>

    <xsl:param name="ExtraElementenContext" as="node()?"><xsl:document /></xsl:param>
	<xsl:param name="PreValueOverridesContext" as="node()?"><xsl:document /></xsl:param>

    <xsl:param name="debug" as="xs:string" select="'false'" />
	<xsl:variable name="debugSerializeParams" as="map(xs:string, item()?)">
		<xsl:map>
			<xsl:map-entry key="'indent'" select="true()"/>
		</xsl:map>
	</xsl:variable>

	<xsl:key name="valueOverrideKey" match="valueOverrides" use="key" />

	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>

	<xsl:template match="/">
		<xsl:apply-templates />
	</xsl:template>

	<xsl:template match="*[contains(text(), '${extraElementen:')]">
		<xsl:if test="$debug = 'true'"><xsl:comment expand-text="yes"> matchOnSubstitutionSection: [{serialize(., $debugSerializeParams)}] </xsl:comment></xsl:if>

		<xsl:variable name="substitutionAction" select="substring-before(substring-after(., '${extraElementen:'), '}')" />
		<xsl:if test="$debug = 'true'"><xsl:comment expand-text="yes"> substitutionAction: [{serialize($substitutionAction, $debugSerializeParams)}] </xsl:comment></xsl:if>

		<xsl:variable name="resolvedSubstitutionAction">
			<xsl:call-template name="resolveSubstitutionAction">
				<xsl:with-param name="action" as="xs:string" select="$substitutionAction" />
			</xsl:call-template>
		</xsl:variable>
		<xsl:if test="$debug = 'true'"><xsl:comment expand-text="yes"> resolvedSubstitutionSection: [{serialize($resolvedSubstitutionAction, $debugSerializeParams)}] </xsl:comment></xsl:if>

		<!-- In ZDS usually the absence of an element means: ignore it/keep it the same as it was before. An empty element with xsi:nil="true" StUF:noValue="geenWaarde" is an explicit "set as empty value".
			For UpdateZaak ZGW PATCH we need to makes sure that the absence of an extraElement is handled as if the valueOverride doesn't exist. Without this logic the absence of extraElement would overwrite
			defaults or composite values from the ZDS to ZGW translation with an empty string. Therefor, if the extraElement is absent or empty, copy the pre-valueOverride value back in, unless it has been explicitly
			flagged to be set to an empty string with xsi:nil="true" StUF:noValue="geenWaarde" -->
		<xsl:choose>
			<xsl:when test="$resolvedSubstitutionAction/*:extraElement/@*:nil = 'true' or $resolvedSubstitutionAction/*:extraElement/text() != ''">
				<xsl:copy select=".">
					<xsl:value-of select="$resolvedSubstitutionAction/*:extraElement" />
				</xsl:copy>
			</xsl:when>
			<xsl:otherwise>
					<xsl:if test="$debug = 'true'"><xsl:comment expand-text="yes"> xpath: [{serialize(path(), $debugSerializeParams)}] </xsl:comment></xsl:if>
					<xsl:evaluate context-item="$PreValueOverridesContext" xpath="path()"></xsl:evaluate>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="resolveSubstitutionAction">
		<xsl:param name="action" as="xs:string" />

		<xsl:variable name="xpathExpression" as="xs:string" select="concat('//*:extraElementen/*:extraElement[@*:naam=''', $action, ''']')" />
		<xsl:if test="$debug = 'true'"><xsl:comment expand-text="yes"> xpathExpression: [{serialize($xpathExpression, $debugSerializeParams)}] </xsl:comment></xsl:if>

		<xsl:variable name="xpathExpressionResult"><xsl:evaluate xpath="$xpathExpression" context-item="$ExtraElementenContext" /></xsl:variable>
		<xsl:if test="$debug = 'true'"><xsl:comment expand-text="yes"> xpathExpressionnResult: [{serialize($xpathExpressionResult, $debugSerializeParams)}] </xsl:comment></xsl:if>

		<xsl:copy-of select="$xpathExpressionResult" />
	</xsl:template>
</xsl:stylesheet>