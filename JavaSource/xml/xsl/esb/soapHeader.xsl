<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<!-- key elements -->
	<xsl:param name="businessDomain" />
	<xsl:param name="serviceName" />
	<xsl:param name="serviceContext" />
	<xsl:param name="serviceContextVersion">1</xsl:param>
	<xsl:param name="operationName" />
	<xsl:param name="operationVersion">1</xsl:param>
	<xsl:param name="paradigm" />
	<xsl:param name="applicationName" />
	<xsl:param name="applicationFunction" />
	<xsl:param name="messagingLayer">ESB</xsl:param>
	<xsl:param name="serviceLayer" />
	<!-- other elements -->
	<xsl:param name="fromId" />
	<xsl:param name="cpaId" />
	<xsl:param name="conversationId" />
	<xsl:param name="messageId" />
	<xsl:param name="correlationId" />
	<xsl:param name="timestamp" />
	<xsl:template match="/">
		<MessageHeader xmlns="uri://nn.nl/XSD/Generic/MessageHeader/1">
			<From>
				<Id>
					<xsl:value-of select="$fromId" />
				</Id>
			</From>
			<To>
				<Location>
					<xsl:choose>
						<xsl:when test="$messagingLayer='P2P'">
							<xsl:value-of select="concat($messagingLayer, '.', $businessDomain, '.', $applicationName, '.', $applicationFunction, '.', $paradigm)"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat($messagingLayer, '.', $businessDomain, '.', $serviceLayer, '.', $serviceName, '.', $serviceContext, '.', $serviceContextVersion, '.', $operationName, '.', $operationVersion, '.', $paradigm)"/>
						</xsl:otherwise>
					</xsl:choose>
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
				<xsl:if test="string-length($correlationId)&gt;0">
					<CorrelationId>
						<xsl:value-of select="$correlationId" />
					</CorrelationId>
				</xsl:if>
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
