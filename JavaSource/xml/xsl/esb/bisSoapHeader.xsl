<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<xsl:param name="fromId" />
	<xsl:param name="conversationId" />
	<xsl:param name="messageId" />
	<xsl:param name="externalRefToMessageId" />
	<xsl:param name="timestamp" />
	<xsl:template match="/">
		<MessageHeader xmlns="http://www.ing.com/CSP/XSD/General/Message_2">
			<From>
				<Id>
					<xsl:value-of select="$fromId" />
				</Id>
			</From>
			<HeaderFields>
				<ConversationId>
					<xsl:value-of select="$conversationId" />
				</ConversationId>
				<MessageId>
					<xsl:value-of select="$messageId" />
				</MessageId>
				<xsl:if test="string-length($externalRefToMessageId)&gt;0">
					<ExternalRefToMessageId>
						<xsl:value-of select="$externalRefToMessageId" />
					</ExternalRefToMessageId>
				</xsl:if>
				<Timestamp>
					<xsl:value-of select="$timestamp" />
				</Timestamp>
			</HeaderFields>
		</MessageHeader>
	</xsl:template>
</xsl:stylesheet>
