<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="text" indent="no"/>
	<!-- namespaces (and prefixes) in the input message are removed before this stylesheet is executed -->
	<xsl:variable name="dot">.</xsl:variable>
	<xsl:variable name="us">_</xsl:variable>
	<xsl:template match="/">
		<xsl:variable name="toLoc">
			<xsl:value-of select="Envelope/Header/MessageHeader/To/Location"/>
		</xsl:variable>
		<xsl:if test="string-length($toLoc)&gt;0 and contains($toLoc,$dot)">
			<xsl:variable name="messagingLayer" select="substring-before($toLoc,$dot)"/>
			<xsl:choose>
				<xsl:when test="$messagingLayer='P2P'">
					<!-- $messagingLayer.$businessDomain.$applicationName.$applicationFunction.$paradigm -->
					<xsl:variable name="toLoc2" select="substring-after($toLoc,$dot)"/>
					<xsl:variable name="toLoc3" select="substring-after($toLoc2,$dot)"/>
					<xsl:variable name="applicationName" select="substring-before($toLoc3,$dot)"/>
					<xsl:variable name="toLoc4" select="substring-after($toLoc3,$dot)"/>
					<xsl:variable name="applicationFunction" select="substring-before($toLoc4,$dot)"/>
					<xsl:value-of select="concat($applicationName,$us,$applicationFunction)"/>
				</xsl:when>
				<xsl:otherwise>
					<!-- $messagingLayer.$businessDomain.$serviceLayer.$serviceName.$serviceContext.$serviceContextVersion.$operationName.$operationVersion.$paradigm -->
					<xsl:variable name="toLoc2" select="substring-after($toLoc,$dot)"/>
					<xsl:variable name="toLoc3" select="substring-after($toLoc2,$dot)"/>
					<xsl:variable name="toLoc4" select="substring-after($toLoc3,$dot)"/>
					<xsl:variable name="serviceName" select="substring-before($toLoc4,$dot)"/>
					<xsl:variable name="toLoc5" select="substring-after($toLoc4,$dot)"/>
					<xsl:variable name="serviceContext" select="substring-before($toLoc5,$dot)"/>
					<xsl:variable name="toLoc6" select="substring-after($toLoc5,$dot)"/>
					<xsl:variable name="serviceContextVersion" select="substring-before($toLoc6,$dot)"/>
					<xsl:variable name="toLoc7" select="substring-after($toLoc6,$dot)"/>
					<xsl:variable name="operationName" select="substring-before($toLoc7,$dot)"/>
					<xsl:variable name="toLoc8" select="substring-after($toLoc7,$dot)"/>
					<xsl:variable name="operationVersion" select="substring-before($toLoc8,$dot)"/>
					<xsl:value-of select="concat($operationName,$us,$operationVersion)"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>
