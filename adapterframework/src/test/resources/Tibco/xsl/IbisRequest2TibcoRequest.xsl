<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes"/>
	<xsl:param name="fromId"/>
	<xsl:param name="messageId"/>
	<xsl:param name="timeStamp"/>
	<xsl:param name="ibisRequestNamespace"/>
	<xsl:param name="tibcoRequestNamespace"/>
	<xsl:param name="tibcoRequestSubNamespace"/>

	<xsl:template match="/">
		<Envelope xmlns="http://schemas.xmlsoap.org/soap/envelope/">
			<Body>
				<MessageHeader xmlns="http://www.ing.com/CSP/XSD/General/Message_2">
					<From>
						<Id><xsl:value-of select="$fromId"/></Id>
					</From>
					<HeaderFields>
						<ConversationId><xsl:value-of select="*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='MessageHeader']/*[local-name()='HeaderFields']/*[local-name()='ConversationId']"/></ConversationId>
						<MessageId><xsl:value-of select="$messageId"/></MessageId>
						<ExternalRefToMessageId><xsl:value-of select="*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='MessageHeader']/*[local-name()='HeaderFields']/*[local-name()='MessageId']"/></ExternalRefToMessageId>
						<Timestamp><xsl:value-of select="$timeStamp"/></Timestamp>
					</HeaderFields>
				</MessageHeader>
				<xsl:element name="Request" namespace="{$tibcoRequestNamespace}">
					<xsl:if test="*[namespace-uri()=$ibisRequestNamespace]">
						<xsl:apply-templates/>
					</xsl:if>
				</xsl:element>
			</Body>
		</Envelope>
	</xsl:template>

	<xsl:template match="*[namespace-uri()=$ibisRequestNamespace]">
		<xsl:element name="{name()}" namespace="{$tibcoRequestSubNamespace}">
			<xsl:copy-of select="@*"/>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>

	<xsl:template match="node()|@*">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="comment()|processing-instruction()">
		<xsl:copy>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>

</xsl:stylesheet>		
