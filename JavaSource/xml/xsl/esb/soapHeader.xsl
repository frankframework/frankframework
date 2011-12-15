<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<xsl:param name="fromId" />
	<xsl:param name="locationTo" />
	<xsl:param name="cpaId" />
	<xsl:param name="conversationId" />
	<xsl:param name="messageId" />
	<xsl:param name="correlationId" />
	<xsl:param name="timestamp" />
	<xsl:param name="serviceName" />
	<xsl:param name="serviceContext" />
	<xsl:param name="paradigm" />
	<xsl:param name="operationName" />
	<xsl:param name="operationVersion" />
	<xsl:template match="/">
		<MessageHeader xmlns="uri://nn.nl/XSD/Generic/MessageHeader/1">
			<From>
				<Id>
					<xsl:value-of select="$fromId" />
				</Id>
			</From>
			<To>
				<Location>
					<xsl:value-of select="$locationTo" />
				</Location>
			</To>
			<HeaderFields>
				<CPAId>
					<xsl:value-of select="$cpaId" />
				</CPAId>
				<ConversationId>
					<xsl:value-of select="$conversationId" />
				</ConversationId>
				<MessageId>
					<xsl:value-of select="$messageId" />
				</MessageId>
				<CorrelationId>
					<xsl:value-of select="$correlationId" />
				</CorrelationId>
				<Timestamp>
					<xsl:value-of select="$timestamp" />
				</Timestamp>
			</HeaderFields>
			<Service>
				<Name>
					<xsl:value-of select="$serviceName" />
				</Name>
				<Context>
					<xsl:value-of select="$serviceContext" />
				</Context>
				<Action>
					<Paradigm>
						<xsl:value-of select="$paradigm" />
					</Paradigm>
					<Name>
						<xsl:value-of select="$operationName" />
					</Name>
					<Version>
						<xsl:value-of select="$operationVersion" />
					</Version>
				</Action>
			</Service>
		</MessageHeader>
	</xsl:template>
</xsl:stylesheet>
