<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
	xmlns:eb="http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd"
	xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">

	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>

	<xsl:param name="cpaFilePath"/>
	<xsl:param name="cpaChannelId"/>
	<xsl:param name="conversationId"/>
	<xsl:param name="action"/>
	<xsl:param name="refToMessageId"/>

	<xsl:template match="/">
		<eb:MessageHeader soap:mustUnderstand="1" eb:version="2.0">
			<eb:From>
				<eb:PartyId>
					<xsl:attribute name="eb:type">
						<xsl:value-of select="'urn:osb:oin'"/>
					</xsl:attribute>
					<xsl:value-of select="00000000000000000001"/>
				</eb:PartyId>
			</eb:From>
			<eb:To>
				<eb:PartyId>
					<xsl:attribute name="eb:type">
						<xsl:value-of select="'urn:osb:oin'"/>
					</xsl:attribute>
					<xsl:value-of select="00000003577777480000"/>
				</eb:PartyId>
			</eb:To>
			<eb:ConversationId>
				<xsl:value-of select="normalize-space($conversationId)"/>
			</eb:ConversationId>
			<eb:Action>
				<xsl:value-of select="$action"/>
			</eb:Action>
			<eb:MessageData>
				<eb:MessageId>
					<xsl:value-of select="'auto-20260806T130431134111Z-2863'"/>
				</eb:MessageId>
				<eb:Timestamp>
					<xsl:value-of select="'2026-08-06T13:04:31.134Z'"/>
				</eb:Timestamp>
				<xsl:if test="string-length($refToMessageId) &gt; 0">
					<eb:RefToMessageId>
						<xsl:value-of select="$refToMessageId"/>
					</eb:RefToMessageId>
				</xsl:if>
				<eb:TimeToLive>
					<xsl:value-of select="'2026-08-14T13:04:31.134Z'"/>
				</eb:TimeToLive>
			</eb:MessageData>
			<eb:DuplicateElimination/>
		</eb:MessageHeader>
	</xsl:template>
</xsl:stylesheet>
