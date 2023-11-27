<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.frankframework.org/tom" version="1.0">
	<xsl:output method="xml" indent="yes" />
	<xsl:template match="/">
		<xsl:apply-templates select="*|comment()|processing-instruction()" />
	</xsl:template>
	<xsl:template match="*|@*|comment()|processing-instruction()">
		<xsl:choose>
			<xsl:when test="name()='Id' and parent::*[name()='InternalAgreement']">
				<Id>
					<xsl:variable name="id" select="." />
					<xsl:variable name="len" select="string-length($id)" />
					<xsl:if test="number($len)&gt;2">
						<xsl:variable name="id1" select="substring($id,1,$len - 2)" />
						<xsl:variable name="id2" select="substring($id,$len - 1)" />
						<xsl:if test="number($id2)&lt;10">
							<xsl:value-of select="concat($id1,format-number(number($id2) + 1,'00'))" />
						</xsl:if>
					</xsl:if>
				</Id>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="copy" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template name="copy">
		<xsl:copy>
			<xsl:apply-templates select="*|@*|comment()|processing-instruction()|text()" />
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>
