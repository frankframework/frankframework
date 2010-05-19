<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes" />
	<!--
		This XSLT copies all nodes except:
		- elements with the active attribute which has a value different from true (not case sensitive)
		- active attributes
	-->
	<xsl:template match="/">
		<xsl:apply-templates select="*|@*|comment()|processing-instruction()" />
	</xsl:template>
	<xsl:template match="*">
		<xsl:choose>
			<xsl:when test="@active">
				<xsl:variable name="activeLC" select="translate(@active,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')" />
				<xsl:if test="$activeLC='true'">
					<xsl:call-template name="copy" />
				</xsl:if>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="copy" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="@*">
		<xsl:if test="name()!='active'">
			<xsl:call-template name="copy" />
		</xsl:if>
	</xsl:template>
	<xsl:template match="comment()|processing-instruction()">
		<xsl:call-template name="copy" />
	</xsl:template>
	<xsl:template name="copy">
		<xsl:copy>
			<xsl:apply-templates select="*|@*|comment()|processing-instruction()|text()" />
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>
