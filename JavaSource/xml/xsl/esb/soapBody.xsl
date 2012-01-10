<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<xsl:param name="errorCode" />
	<xsl:param name="errorReason" />
	<xsl:param name="errorDetailCode" />
	<xsl:param name="errorDetailText" />
	<xsl:param name="serviceName" />
	<xsl:param name="serviceContext" />
	<xsl:param name="operationName" />
	<xsl:param name="operationVersion">1</xsl:param>
	<xsl:param name="paradigm" />
	<!--
		if $errorCode is empty then
		 - the complete input message is copied
		 - a result tag is added as last child of the root tag if it doesn't exist and $paradigm equals 'Response'
		if $errorCode is not empty then
		 - the root tag of the input message is copied
		 - a result tag is wrapped in this copied root tag
	-->
	<xsl:variable name="result_exists">
		<xsl:choose>
			<xsl:when test="*/*[local-name(.)='Result']">true</xsl:when>
			<xsl:otherwise>false</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<xsl:template match="/">
		<xsl:choose>
			<xsl:when test="string-length($errorCode)=0">
				<xsl:apply-templates select="*|@*|comment()|processing-instruction()" mode="ok" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates select="*|@*|comment()|processing-instruction()" mode="error" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="*|@*|comment()|processing-instruction()|text()" mode="ok">
		<xsl:copy>
			<xsl:apply-templates select="*|@*|comment()|processing-instruction()|text()" mode="ok" />
			<xsl:if test="not(parent::*) and $result_exists='false' and $paradigm='Response'">
				<xsl:call-template name="Result" />
			</xsl:if>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="*|@*|comment()|processing-instruction()|text()" mode="error">
		<xsl:if test="not(parent::*)">
			<xsl:copy>
				<xsl:apply-templates select="*|@*|comment()|processing-instruction()|text()" mode="error" />
				<xsl:call-template name="Result" />
			</xsl:copy>
		</xsl:if>
	</xsl:template>
	<xsl:template name="Result">
		<Result xmlns="http://nn.nl/XSD/Generic/MessageHeader/1">
			<xsl:choose>
				<xsl:when test="string-length($errorCode)=0">
					<Status>OK</Status>
				</xsl:when>
				<xsl:otherwise>
					<Status>ERROR</Status>
					<ErrorList>
						<Error>
							<Code>
								<xsl:value-of select="$errorCode" />
							</Code>
							<Reason>
								<xsl:choose>
									<xsl:when test="string-length($errorReason)=0">
										<xsl:choose>
											<xsl:when test="$errorCode='ERR6002'">Service Interface Request Time Out</xsl:when>
											<xsl:when test="$errorCode='ERR6003'">Invalid Request Message</xsl:when>
											<xsl:when test="$errorCode='ERR6004'">Invalid Backend system response</xsl:when>
											<xsl:when test="$errorCode='ERR6005'">Backend system failure response</xsl:when>
											<xsl:when test="$errorCode='ERR6999'">Unspecified Errors</xsl:when>
										</xsl:choose>
									</xsl:when>
									<xsl:otherwise>
										<xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
										<xsl:value-of select="$errorReason" />
										<xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
									</xsl:otherwise>
								</xsl:choose>
							</Reason>
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
							<xsl:if test="string-length($errorDetailCode)&gt;0">
								<DetailList>
									<Detail>
										<Code>
											<xsl:value-of select="$errorDetailCode" />
										</Code>
										<xsl:if test="string-length($errorDetailText)&gt;0">
											<Text>
												<xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
												<xsl:value-of select="$errorDetailText" />
												<xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
											</Text>
										</xsl:if>
									</Detail>
								</DetailList>
							</xsl:if>
						</Error>
					</ErrorList>
				</xsl:otherwise>
			</xsl:choose>
		</Result>
	</xsl:template>
</xsl:stylesheet>
