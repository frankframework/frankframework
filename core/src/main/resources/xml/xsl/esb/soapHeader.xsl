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
	<xsl:param name="namespace">http://nn.nl/XSD/Generic/MessageHeader/1</xsl:param>
	<xsl:param name="fromId" />
	<xsl:param name="cpaId" />
	<xsl:param name="conversationId" />
	<xsl:param name="messageId" />
	<xsl:param name="correlationId" />
	<xsl:param name="externalRefToMessageId" />
	<xsl:param name="timestamp" />
	<xsl:template match="/">
		<xsl:element name="MessageHeader" namespace="{$namespace}">
			<xsl:element name="From" namespace="{$namespace}">
				<xsl:element name="Id" namespace="{$namespace}">
					<xsl:value-of select="$fromId" />
				</xsl:element>
			</xsl:element>
			<xsl:element name="To" namespace="{$namespace}">
				<xsl:element name="Location" namespace="{$namespace}">
					<xsl:choose>
						<xsl:when test="$messagingLayer='P2P'">
							<xsl:value-of select="concat($messagingLayer, '.', $businessDomain, '.', $applicationName, '.', $applicationFunction, '.', $paradigm)"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat($messagingLayer, '.', $businessDomain, '.', $serviceLayer, '.', $serviceName, '.', $serviceContext, '.', $serviceContextVersion, '.', $operationName, '.', $operationVersion, '.', $paradigm)"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:element>
			</xsl:element>
			<xsl:element name="HeaderFields" namespace="{$namespace}">
				<xsl:element name="CPAId" namespace="{$namespace}">
					<xsl:value-of select="$cpaId" />
				</xsl:element>
				<xsl:element name="ConversationId" namespace="{$namespace}">
					<xsl:value-of select="$conversationId" />
				</xsl:element>
				<xsl:element name="MessageId" namespace="{$namespace}">
					<xsl:value-of select="$messageId" />
				</xsl:element>
				<xsl:if test="string-length($correlationId)&gt;0">
					<xsl:element name="CorrelationId" namespace="{$namespace}">
						<xsl:value-of select="$correlationId" />
					</xsl:element>
				</xsl:if>
				<xsl:if test="string-length($externalRefToMessageId)&gt;0">
					<xsl:element name="ExternalRefToMessageId" namespace="{$namespace}">
						<xsl:value-of select="$externalRefToMessageId" />
					</xsl:element>
				</xsl:if>
				<xsl:element name="Timestamp" namespace="{$namespace}">
					<xsl:value-of select="$timestamp" />
				</xsl:element>
			</xsl:element>
			<xsl:element name="Service" namespace="{$namespace}">
				<xsl:element name="Name" namespace="{$namespace}">
					<xsl:value-of select="$serviceName" />
				</xsl:element>
				<xsl:element name="Context" namespace="{$namespace}">
					<xsl:value-of select="$serviceContext" />
				</xsl:element>
				<xsl:element name="Action" namespace="{$namespace}">
					<xsl:element name="Paradigm" namespace="{$namespace}">
						<xsl:value-of select="$paradigm" />
					</xsl:element>
					<xsl:element name="Name" namespace="{$namespace}">
						<xsl:value-of select="$operationName" />
					</xsl:element>
					<xsl:element name="Version" namespace="{$namespace}">
						<xsl:value-of select="$operationVersion" />
					</xsl:element>
				</xsl:element>
			</xsl:element>
		</xsl:element>
	</xsl:template>
</xsl:stylesheet>
