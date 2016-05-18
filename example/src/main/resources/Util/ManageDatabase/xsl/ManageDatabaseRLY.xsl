<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>
	<xsl:param name="returnResults">true</xsl:param>
	<xsl:template match="/">
		<manageDatabaseRLY>
			<completionInformation>
				<returnCode>
					<xsl:choose>
						<xsl:when test="results/result[result=false()]">NOT_OK</xsl:when>
						<xsl:otherwise>OK</xsl:otherwise>
					</xsl:choose>
				</returnCode>
			</completionInformation>
			<xsl:if test="$returnResults='true'">
				<xsl:apply-templates select="*|@*|comment()|processing-instruction()"/>
			</xsl:if>
		</manageDatabaseRLY>
	</xsl:template>
	<xsl:template match="*|@*|comment()|processing-instruction()">
		<xsl:copy>
			<xsl:apply-templates select="*|@*|comment()|processing-instruction()|text()"/>
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>
