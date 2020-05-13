<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0" 
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	exclude-result-prefixes="xsi">
	<xsl:output method="xml" indent="yes" />

	<xsl:variable name="lookup" select="document('uglify_lookup.xml')"/>

	<xsl:template match="*">
		<xsl:variable name="currentName" select="local-name()"/>
		<xsl:variable name="currentType" select="$lookup/Elements/Element[Name = $currentName]/Type"/>
		<xsl:variable name="currentClassName" select="$lookup/Elements/Element[Name = $currentName]/ClassName"/>
		<xsl:choose>
			<xsl:when test="string-length($currentType) > 0 ">
				<xsl:element name="{$currentType}">
					<xsl:if test="string-length($currentClassName) > 0">
						<xsl:attribute name="className"><xsl:value-of select="$currentClassName"></xsl:value-of></xsl:attribute>
					</xsl:if>
					<xsl:apply-templates select="@*|node()[not(self::Exit)]|comment()|processing-instruction()|text()" />
					<xsl:if test="local-name() = 'Pipeline'">
						<xsl:element name="exits">
							<xsl:apply-templates select="Exit"/>
						</xsl:element>
					</xsl:if>
				</xsl:element>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="copy" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="@*|comment()|processing-instruction()">
		<xsl:call-template name="copy" />
	</xsl:template>

	<xsl:template name="copy">
		<xsl:copy>
			<xsl:apply-templates select="*|@*|comment()|processing-instruction()|text()" />
		</xsl:copy>
	</xsl:template>
	
	<!-- Filter out the XMLSchema Instance namespace information used for validation of Beautiful syntax -->
	<xsl:template match="@xsi:*|xsi:*"/>

</xsl:stylesheet>
