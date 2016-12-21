<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<xsl:param name="conversationId" />
	<xsl:variable name="ns" select="'http://api.nn.nl/MessageHeader'" />
	<xsl:template match="/">
		<!-- only create a MessageHeader when the parameter conversationId is filled -->
		<xsl:if test="string-length($conversationId)&gt;0">
			<xsl:element name="MessageHeader" namespace="{$ns}">
				<xsl:element name="HeaderFields" namespace="{$ns}">
					<xsl:element name="ConversationId" namespace="{$ns}">
						<xsl:value-of select="$conversationId" />
					</xsl:element>
				</xsl:element>
			</xsl:element>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>
