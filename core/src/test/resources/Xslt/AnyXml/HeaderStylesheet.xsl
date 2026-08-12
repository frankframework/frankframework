<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
	xmlns:eb="http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd"
	xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
	xmlns:tns="http://www.oasis-open.org/committees/ebxml-cppa/schema/cpp-cpa-2_0.xsd"
	xmlns:xs="http://www.w3.org/2001/XMLSchema">

	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>

	<xsl:param name="cpaFilePath"/>
	<xsl:param name="cpaChannelId"/>
	<xsl:param name="conversationId"/>
	<xsl:param name="action"/>
	<xsl:param name="refToMessageId"/>

	<xsl:variable name="cpaDocument"
		select="if (string-length(normalize-space($cpaFilePath)) gt 0 and doc-available($cpaFilePath)) then doc($cpaFilePath) else ()"/>
	<xsl:variable name="cpaMessagingCharacteristics"
		select="$cpaDocument/tns:CollaborationProtocolAgreement//tns:DeliveryChannel[@tns:channelId = $cpaChannelId]/tns:MessagingCharacteristics[1]"/>
	<xsl:variable name="effectiveDuplicateElimination"
		select="lower-case(normalize-space(string($cpaMessagingCharacteristics/@tns:duplicateElimination)))"/>

	<xsl:variable name="effectiveCpaId"
		select="string($cpaDocument/tns:CollaborationProtocolAgreement/@tns:cpaid)"/>
	<xsl:variable name="cpaFromPartyInfo"
		select="$cpaDocument/tns:CollaborationProtocolAgreement/tns:PartyInfo[tns:DeliveryChannel/@tns:channelId = $cpaChannelId]"/>
	<xsl:variable name="cpaFromCollaborationRole"
		select="$cpaFromPartyInfo/tns:CollaborationRole[tns:ServiceBinding/tns:CanSend/tns:ThisPartyActionBinding[@tns:action = $action][tns:ChannelId = $cpaChannelId]]"/>
	<xsl:variable name="cpaOtherPartyBindingId"
		select="string($cpaFromCollaborationRole/tns:ServiceBinding/tns:CanSend/tns:OtherPartyActionBinding)"/>
	<xsl:variable name="cpaToPartyInfo"
		select="$cpaDocument/tns:CollaborationProtocolAgreement/tns:PartyInfo[not(tns:DeliveryChannel/@tns:channelId = $cpaChannelId)]"/>
	<xsl:variable name="cpaToCollaborationRole"
		select="$cpaToPartyInfo/tns:CollaborationRole[tns:ServiceBinding//tns:ThisPartyActionBinding/@tns:id = $cpaOtherPartyBindingId]"/>
	<xsl:variable name="effectiveFromPartyId"
		select="string($cpaFromPartyInfo/tns:PartyId)"/>
	<xsl:variable name="effectiveFromPartyType"
		select="string($cpaFromPartyInfo/tns:PartyId/@tns:type)"/>
	<xsl:variable name="effectiveFromRole"
		select="string($cpaFromCollaborationRole/tns:Role/@tns:name)"/>
	<xsl:variable name="effectiveToPartyId"
		select="string($cpaToPartyInfo/tns:PartyId)"/>
	<xsl:variable name="effectiveToPartyType"
		select="string($cpaToPartyInfo/tns:PartyId/@tns:type)"/>
	<xsl:variable name="effectiveToRole"
		select="string($cpaToCollaborationRole/tns:Role/@tns:name)"/>
	<xsl:variable name="effectiveService"
		select="if (string-length(normalize-space(string($cpaFromCollaborationRole/tns:ServiceBinding/tns:Service))) gt 0)
                then string($cpaFromCollaborationRole/tns:ServiceBinding/tns:Service)
                else 'urn:oasis:names:tc:ebxml-msg:service'"/>
	<xsl:variable name="effectiveServiceType"
		select="string($cpaFromCollaborationRole/tns:ServiceBinding/tns:Service/@tns:type)"/>

	<xsl:variable name="cpaDocExchangeId"
		select="string($cpaFromPartyInfo/tns:DeliveryChannel[@tns:channelId = $cpaChannelId]/@tns:docExchangeId)"/>
	<xsl:variable name="cpaPersistDuration"
		select="string($cpaFromPartyInfo/tns:DocExchange[@tns:docExchangeId = $cpaDocExchangeId]/tns:ebXMLSenderBinding/tns:PersistDuration)"/>
	<xsl:variable name="utcNow"
		select="adjust-dateTime-to-timezone(current-dateTime(), xs:dayTimeDuration('PT0H'))"/>
	<xsl:variable name="idTimestampToken"
		select="format-dateTime($utcNow, '[Y0001][M01][D01]T[H01][m01][s01][f]Z')"/>
	<xsl:variable name="idEntropyToken"
		select="string(sum(string-to-codepoints(concat($action, '|', $conversationId, '|', $refToMessageId, '|', $idTimestampToken))))"/>
	<xsl:variable name="effectiveMessageId"
		select="concat('auto-', $idTimestampToken, '-', $idEntropyToken)"/>
	<xsl:variable name="effectiveTimestamp"
		select="format-dateTime($utcNow, '[Y0001]-[M01]-[D01]T[H01]:[m01]:[s01].[f,3-3]Z')"/>
	<xsl:variable name="effectiveTimeToLive"
		select="if (string-length(normalize-space($cpaPersistDuration)) gt 0)
                then format-dateTime($utcNow + xs:dayTimeDuration($cpaPersistDuration), '[Y0001]-[M01]-[D01]T[H01]:[m01]:[s01].[f,3-3]Z')
                else ''"/>

	<xsl:template match="/">
		<eb:MessageHeader soap:mustUnderstand="1" eb:version="2.0">
			<eb:From>
				<eb:PartyId>
					<xsl:if test="string-length($effectiveFromPartyType) &gt; 0">
						<xsl:attribute name="eb:type">
							<xsl:value-of select="$effectiveFromPartyType"/>
						</xsl:attribute>
					</xsl:if>
					<xsl:value-of select="$effectiveFromPartyId"/>
				</eb:PartyId>
				<xsl:if test="string-length($effectiveFromRole) &gt; 0">
					<eb:Role>
						<xsl:value-of select="$effectiveFromRole"/>
					</eb:Role>
				</xsl:if>
			</eb:From>
			<eb:To>
				<eb:PartyId>
					<xsl:if test="string-length($effectiveToPartyType) &gt; 0">
						<xsl:attribute name="eb:type">
							<xsl:value-of select="$effectiveToPartyType"/>
						</xsl:attribute>
					</xsl:if>
					<xsl:value-of select="$effectiveToPartyId"/>
				</eb:PartyId>
				<xsl:if test="string-length($effectiveToRole) &gt; 0">
					<eb:Role>
						<xsl:value-of select="$effectiveToRole"/>
					</eb:Role>
				</xsl:if>
			</eb:To>
			<eb:CPAId>
				<xsl:value-of select="$effectiveCpaId"/>
			</eb:CPAId>
			<eb:ConversationId>
				<xsl:value-of select="normalize-space($conversationId)"/>
			</eb:ConversationId>
			<eb:Service>
				<xsl:if test="string-length($effectiveServiceType) &gt; 0">
					<xsl:attribute name="eb:type">
						<xsl:value-of select="$effectiveServiceType"/>
					</xsl:attribute>
				</xsl:if>
				<xsl:value-of select="$effectiveService"/>
			</eb:Service>
			<eb:Action>
				<xsl:value-of select="$action"/>
			</eb:Action>
			<eb:MessageData>
				<eb:MessageId>
					<xsl:value-of select="$effectiveMessageId"/>
				</eb:MessageId>
				<eb:Timestamp>
					<xsl:value-of select="$effectiveTimestamp"/>
				</eb:Timestamp>
				<xsl:if test="string-length($refToMessageId) &gt; 0">
					<eb:RefToMessageId>
						<xsl:value-of select="$refToMessageId"/>
					</eb:RefToMessageId>
				</xsl:if>
				<xsl:if test="string-length($effectiveTimeToLive) &gt; 0">
					<eb:TimeToLive>
						<xsl:value-of select="$effectiveTimeToLive"/>
					</eb:TimeToLive>
				</xsl:if>
			</eb:MessageData>
			<xsl:if test="$effectiveDuplicateElimination=('true','always')">
				<eb:DuplicateElimination/>
			</xsl:if>
		</eb:MessageHeader>
	</xsl:template>
</xsl:stylesheet>
