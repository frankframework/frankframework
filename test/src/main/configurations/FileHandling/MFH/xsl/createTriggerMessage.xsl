<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" indent="no" omit-xml-declaration="yes" />
	
	<xsl:param name="CanonicalName"/>
	<xsl:param name="Filename"/>
	<xsl:param name="MFHFlowId"/>
	<xsl:param name="LegacyFlowId"/>
	<xsl:param name="LocalTransactionID"/>
	<xsl:param name="OriginalFilename"/>

	<!-- Still needs to be improved-->
	<xsl:template match="/">
		<Message>
			<Filename><xsl:value-of select="$Filename"/></Filename>
			<OriginalFilename><xsl:value-of select="$OriginalFilename"/></OriginalFilename>
			<MFHFlowId><xsl:value-of select="$MFHFlowId"/></MFHFlowId>
			<CanonicalName><xsl:value-of select="$CanonicalName"/></CanonicalName>
			<LegacyMessage>
				<OnCompletedTransferNotify_Action>
					<TransferFlowId><xsl:value-of select="$LegacyFlowId"/></TransferFlowId>
					<UserData><xsl:value-of select="$Filename"/></UserData>
					<ClientFilename><xsl:value-of select="$CanonicalName"/></ClientFilename>
					<ServerFilename><xsl:value-of select="$CanonicalName"/></ServerFilename>
					<LocalTransactionID><xsl:value-of select="$LocalTransactionID"/></LocalTransactionID>
				</OnCompletedTransferNotify_Action>
			</LegacyMessage>
		</Message>
	</xsl:template>

</xsl:stylesheet>
