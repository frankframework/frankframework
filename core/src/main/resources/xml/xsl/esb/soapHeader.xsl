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
	<xsl:param name="mode"/>
	<xsl:param name="cmhVersion"/>
	<xsl:param name="namespace"/>
	<xsl:param name="fromId" />
	<xsl:param name="cpaId" />
	<xsl:param name="conversationId" />
	<xsl:param name="messageId" />
	<xsl:param name="correlationId" />
	<xsl:param name="externalRefToMessageId" />
	<xsl:param name="timestamp" />
	<xsl:param name="transactionId" />
	<xsl:variable name="ns">
		<xsl:choose>
			<xsl:when test="string-length($namespace)=0">
				<xsl:choose>
					<xsl:when test="number($cmhVersion)=2">http://nn.nl/XSD/Generic/MessageHeader/2</xsl:when>
					<xsl:otherwise>http://nn.nl/XSD/Generic/MessageHeader/1</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$namespace"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<xsl:template match="/">
		<xsl:element name="MessageHeader" namespace="{$ns}">
			<xsl:element name="From" namespace="{$ns}">
				<xsl:element name="Id" namespace="{$ns}">
					<xsl:value-of select="$fromId" />
				</xsl:element>
			</xsl:element>
			<xsl:element name="To" namespace="{$ns}">
				<xsl:element name="Location" namespace="{$ns}">
					<xsl:choose>
						<xsl:when test="$messagingLayer='P2P'">
							<xsl:value-of select="concat($messagingLayer, '.', $businessDomain, '.', $applicationName, '.', $applicationFunction, '.', $paradigm)"/>
						</xsl:when>
						<xsl:when test="string-length($serviceContext)=0">
							<xsl:value-of select="concat($messagingLayer, '.', $businessDomain, '.', $serviceLayer, '.', $serviceName, '.', $serviceContextVersion, '.', $operationName, '.', $operationVersion, '.', $paradigm)"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat($messagingLayer, '.', $businessDomain, '.', $serviceLayer, '.', $serviceName, '.', $serviceContext, '.', $serviceContextVersion, '.', $operationName, '.', $operationVersion, '.', $paradigm)"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:element>
			</xsl:element>
			<xsl:element name="HeaderFields" namespace="{$ns}">
				<xsl:element name="CPAId" namespace="{$ns}">
					<xsl:value-of select="$cpaId" />
				</xsl:element>
				<xsl:element name="ConversationId" namespace="{$ns}">
					<xsl:value-of select="$conversationId" />
				</xsl:element>
				<xsl:element name="MessageId" namespace="{$ns}">
					<xsl:value-of select="$messageId" />
				</xsl:element>
				<xsl:if test="string-length($correlationId)&gt;0">
					<xsl:element name="CorrelationId" namespace="{$ns}">
						<xsl:value-of select="$correlationId" />
					</xsl:element>
				</xsl:if>
				<xsl:if test="string-length($externalRefToMessageId)&gt;0">
					<xsl:element name="ExternalRefToMessageId" namespace="{$ns}">
						<xsl:value-of select="$externalRefToMessageId" />
					</xsl:element>
				</xsl:if>
				<xsl:element name="Timestamp" namespace="{$ns}">
					<xsl:value-of select="$timestamp" />
				</xsl:element>
				<xsl:if test="number($cmhVersion)=2 and string-length($transactionId)&gt;0">
					<xsl:element name="TransactionId" namespace="{$ns}">
						<xsl:value-of select="$transactionId" />
					</xsl:element>
				</xsl:if>
			</xsl:element>
			<xsl:element name="Service" namespace="{$ns}">
				<xsl:element name="Name" namespace="{$ns}">
					<xsl:value-of select="$serviceName" />
				</xsl:element>
				<xsl:element name="Context" namespace="{$ns}">
					<xsl:value-of select="$serviceContext" />
				</xsl:element>
				<xsl:element name="Action" namespace="{$ns}">
					<xsl:element name="Paradigm" namespace="{$ns}">
						<xsl:value-of select="$paradigm" />
					</xsl:element>
					<xsl:element name="Name" namespace="{$ns}">
						<xsl:value-of select="$operationName" />
					</xsl:element>
					<xsl:element name="Version" namespace="{$ns}">
						<xsl:value-of select="$operationVersion" />
					</xsl:element>
				</xsl:element>
			</xsl:element>
		</xsl:element>
	</xsl:template>
</xsl:stylesheet>
