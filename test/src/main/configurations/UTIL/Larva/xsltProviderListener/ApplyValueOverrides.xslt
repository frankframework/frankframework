<xsl:stylesheet version="3.0" exclude-result-prefixes="#all" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" omit-xml-declaration="yes" />
    <xsl:strip-space elements="*"/>

    <xsl:param name="valueOverrides" as="node()?"><xsl:document><root /></xsl:document></xsl:param>
    <xsl:param name="mergeWith" as="node()?"><xsl:document /></xsl:param>
    <xsl:param name="defaultMergeCondition" as="xs:string">string-length(.) = 0</xsl:param>
    <xsl:param name="processAsArrayKeys" select="''" as="xs:string" />

    <xsl:param name="debug" as="xs:string" select="'false'" />
	<xsl:variable name="debugSerializeParams" as="map(xs:string, item()?)">
		<xsl:map>
			<xsl:map-entry key="'indent'" select="true()"/>
		</xsl:map>
	</xsl:variable>

	<xsl:key name="valueOverrideKey" match="valueOverrides" use="key" />

	<xsl:template match="/">
		<xsl:if test="$debug = 'true'"><xsl:if test="$debug = 'true'"><xsl:comment expand-text="yes"> input context: [{serialize(/, $debugSerializeParams)}] </xsl:comment></xsl:if></xsl:if>
		<xsl:if test="$debug = 'true'"><xsl:comment expand-text="yes"> mergeWith context: [{serialize($mergeWith, $debugSerializeParams)}] </xsl:comment></xsl:if>
		<xsl:call-template name="merge">
			<xsl:with-param name="inputContext" select="/" />
			<xsl:with-param name="valueOverridesContext" select="$mergeWith" />
			<xsl:with-param name="valueOverrideKey" select="''" />
		</xsl:call-template>
  </xsl:template>

	<xsl:template name="merge">
		<xsl:param name="inputContext" />
		<xsl:param name="valueOverridesContext" />
		<xsl:param name="valueOverrideKey" as="xs:string" />
		<xsl:merge>
            <xsl:merge-source name="inputContext" sort-before-merge="yes" for-each-item="$inputContext" select="*" >
                <xsl:merge-key select="local-name()" exclude-result-prefixes="#all" />
            </xsl:merge-source>
            <xsl:merge-source name="valueOverridesContext" sort-before-merge="yes" for-each-item="$valueOverridesContext" select="*">
                <xsl:merge-key select="local-name()" exclude-result-prefixes="#all"  />
            </xsl:merge-source>
            <xsl:merge-action>
				<xsl:if test="$debug = 'true'"><xsl:comment expand-text="yes"> current-merge-key: [{serialize(current-merge-key(), $debugSerializeParams)}] </xsl:comment></xsl:if>
				<xsl:if test="$debug = 'true'"><xsl:comment expand-text="yes"> current-merge-group: [{serialize(current-merge-group(), $debugSerializeParams)}] </xsl:comment></xsl:if>

				<xsl:variable name="currentValueOverrideKey" select="if ($valueOverrideKey != '') then concat($valueOverrideKey, '.', current-merge-key()) else current-merge-key()" />
				<xsl:if test="$debug = 'true'"><xsl:comment expand-text="yes"> currentValueOverrideKey: [{serialize($currentValueOverrideKey, $debugSerializeParams)}] </xsl:comment></xsl:if>

				<xsl:variable name="valueOverride" select="key('valueOverrideKey', $currentValueOverrideKey, $valueOverrides)" />
				<xsl:if test="$debug = 'true'"><xsl:comment expand-text="yes"> valueOverride: [{serialize($valueOverride, $debugSerializeParams)}] </xsl:comment></xsl:if>
				
				<xsl:choose>
					<xsl:when test="tokenize($processAsArrayKeys, ',') = $currentValueOverrideKey">
						<xsl:if test="$debug = 'true'"><xsl:comment> merge-action-route: [processAsArray bypass] </xsl:comment></xsl:if>

						<xsl:copy-of select="current-merge-group('inputContext')" />
						<xsl:copy-of select="current-merge-group('valueOverridesContext')" />
					</xsl:when>
					<xsl:when test="current-merge-group('inputContext')/* or current-merge-group('valueOverridesContext')/*">
						<xsl:if test="$debug = 'true'"><xsl:comment> merge-action-route: [merge children] </xsl:comment></xsl:if>

						<xsl:element name="{current-merge-key()}">
							<xsl:call-template name="merge">
								<xsl:with-param name="inputContext" select="current-merge-group('inputContext')" />
								<xsl:with-param name="valueOverridesContext" select="current-merge-group('valueOverridesContext')" />
								<xsl:with-param name="valueOverrideKey" select="$currentValueOverrideKey" />
							</xsl:call-template>
						</xsl:element>
					</xsl:when>
					<xsl:when test="current-merge-group('valueOverridesContext')">
						<xsl:if test="$debug = 'true'"><xsl:comment> merge-action-route: [process valueOverride] </xsl:comment></xsl:if>

						<xsl:variable name="valueOverrideCondition" select="if ($valueOverride/condition) then $valueOverride/condition else $defaultMergeCondition" />
						<xsl:if test="$debug = 'true'"><xsl:comment expand-text="yes"> resolvedValueOverrideCondition: [{serialize($valueOverrideCondition, $debugSerializeParams)}] </xsl:comment></xsl:if>

						<xsl:variable name="contextItem">
							<xsl:choose>
								<xsl:when test="current-merge-group('inputContext')">
									<xsl:copy-of select="current-merge-group('inputContext')" />
								</xsl:when>
								<xsl:otherwise><emptyContextItem/></xsl:otherwise>
							</xsl:choose>
						</xsl:variable>
						<xsl:if test="$debug = 'true'"><xsl:comment expand-text="yes"> context-item: [{serialize($contextItem, $debugSerializeParams)}] </xsl:comment></xsl:if>

						<xsl:variable name="valueOverrideConditionResult" as="xs:boolean"><xsl:value-of><xsl:evaluate xpath="$valueOverrideCondition" context-item="$contextItem" as="xs:boolean" /></xsl:value-of></xsl:variable>
						<xsl:if test="$debug = 'true'"><xsl:comment expand-text="yes"> evaluateConditionResult: [{serialize($valueOverrideConditionResult, $debugSerializeParams)}] </xsl:comment></xsl:if>

						<xsl:choose>
							<xsl:when test="$valueOverrideConditionResult = true()">
								<xsl:if test="$debug = 'true'"><xsl:comment> evaluateConditionResult: [true], using merge-group: [valueOverridesContext] </xsl:comment></xsl:if>
								<xsl:copy-of select="current-merge-group('valueOverridesContext')" />
							</xsl:when>
							<xsl:otherwise>
								<xsl:if test="$debug = 'true'"><xsl:comment> evaluateConditionResult: [false], using merge-group: [inputContext] </xsl:comment></xsl:if>
								<xsl:copy-of select="current-merge-group('inputContext')" />
							</xsl:otherwise>
						</xsl:choose>
					</xsl:when>
					<xsl:otherwise>
						<xsl:if test="$debug = 'true'"><xsl:comment> merge-action-route: [inputContext value] </xsl:comment></xsl:if>

						<xsl:copy-of select="current-merge-group('inputContext')" />
					</xsl:otherwise>
				</xsl:choose>
            </xsl:merge-action>
      	</xsl:merge>
	</xsl:template>
</xsl:stylesheet>
