<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>
	<xsl:param name="fromId"/>
	<xsl:param name="conversationId"/>
	<xsl:param name="messageId"/>
	<xsl:param name="timestamp"/>
	<xsl:param name="msgMessageId"/>
	<xsl:param name="msgCorrelationId"/>
	<xsl:param name="msgTimestamp"/>
	<xsl:param name="slotId"/>
	<xsl:param name="msgType"/>
	<xsl:param name="msg"/>
	<xsl:template match="/">
		<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
			<soapenv:Header>
				<MessageHeader xmlns="http://nn.nl/XSD/Generic/MessageHeader/1">
					<From>
						<Id>
							<xsl:value-of select="$fromId"/>
						</Id>
					</From>
					<To>
						<Location>ESB.Infrastructure.US.Log.BusinessLog.2.AudtiLog.1.Action</Location>
					</To>
					<HeaderFields>
						<CPAId>n/a</CPAId>
						<ConversationId>
							<xsl:value-of select="$conversationId"/>
						</ConversationId>
						<MessageId>
							<xsl:value-of select="$messageId"/>
						</MessageId>
						<Timestamp>
							<xsl:value-of select="$timestamp"/>
						</Timestamp>
					</HeaderFields>
					<Service>
						<Name>Log</Name>
						<Context>BusinessLog</Context>
						<Action>
							<Paradigm>Action</Paradigm>
							<Name>AuditLog</Name>
							<Version>1</Version>
						</Action>
					</Service>
				</MessageHeader>
			</soapenv:Header>
			<soapenv:Body>
				<AuditLog_Action xmlns="http://nn.nl/XSD/Infrastructure/Log/BusinessLog/2/AuditLog/1">
					<Header>
						<MessageId>
							<xsl:value-of select="$msgMessageId"/>
						</MessageId>
						<CPAId>
							<xsl:value-of select="$msgCorrelationId"/>
						</CPAId>
						<ApplicationName>
							<xsl:value-of select="$fromId"/>
						</ApplicationName>
						<ApplicationFunction>
							<xsl:value-of select="$slotId"/>
						</ApplicationFunction>
						<Timestamp>
							<xsl:value-of select="$msgTimestamp"/>
						</Timestamp>
					</Header>
					<Message>
						<Text>
							<xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
							<xsl:value-of select="$msg"/>
							<xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
						</Text>
						<Type>
							<xsl:value-of select="$msgType"/>
						</Type>
					</Message>
				</AuditLog_Action>
			</soapenv:Body>
		</soapenv:Envelope>
	</xsl:template>
</xsl:stylesheet>
