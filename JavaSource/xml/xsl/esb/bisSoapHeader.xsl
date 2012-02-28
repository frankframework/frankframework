<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<xsl:param name="namespace">http://www.ing.com/CSP/XSD/General/Message_2</xsl:param>
	<xsl:param name="fromId" />
	<xsl:param name="conversationId" />
	<xsl:param name="messageId" />
	<xsl:param name="externalRefToMessageId" />
	<xsl:param name="timestamp" />
	<xsl:template match="/">
		<xsl:element name="MessageHeader" namespace="{$namespace}">
			<xsl:element name="From" namespace="{$namespace}">
				<xsl:element name="Id" namespace="{$namespace}">
					<xsl:value-of select="$fromId" />
				</xsl:element>
			</xsl:element>
			<xsl:element name="HeaderFields" namespace="{$namespace}">
				<xsl:element name="ConversationId" namespace="{$namespace}">
					<xsl:value-of select="$conversationId" />
				</xsl:element>
				<xsl:element name="MessageId" namespace="{$namespace}">
					<xsl:value-of select="$messageId" />
				</xsl:element>
				<xsl:if test="string-length($externalRefToMessageId)&gt;0">
					<xsl:element name="ExternalRefToMessageId" namespace="{$namespace}">
						<xsl:value-of select="$externalRefToMessageId" />
					</xsl:element>
				</xsl:if>
				<xsl:element name="Timestamp" namespace="{$namespace}">
					<xsl:value-of select="$timestamp" />
				</xsl:element>
			</xsl:element>
		</xsl:element>
	</xsl:template>
</xsl:stylesheet>
