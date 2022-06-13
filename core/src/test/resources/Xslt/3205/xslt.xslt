<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes" />
	<xsl:param name="getPartiesOnAgreementRLY" />
	<xsl:template match="/">
		<xsl:apply-templates select="*|@*|comment()|processing-instruction()" />
	</xsl:template>
	<xsl:template match="*|@*|comment()|processing-instruction()">
		<xsl:copy>
			<xsl:apply-templates select="*|@*|comment()|processing-instruction()|text()" />
			<xsl:if test="name()='PolicyAccount'">
				<xsl:element name="GEVONDEN">Hoi</xsl:element>
				<xsl:for-each select="$getPartiesOnAgreementRLY/PartyAgreementRole">
					<xsl:copy>
						<xsl:apply-templates select="*|@*|comment()|processing-instruction()|text()" />
					</xsl:copy>
				</xsl:for-each>
			</xsl:if>
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>