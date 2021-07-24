<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>
	<xsl:param name="mode"/>
	<xsl:param name="cmhVersion"/>
	<xsl:param name="namespace"/>
	<xsl:param name="errorCode"/>
	<xsl:param name="errorReason"/>
	<xsl:param name="errorDetailCode"/>
	<xsl:param name="errorDetailText"/>
	<xsl:param name="errorDetailsXml"/>
	<xsl:param name="addErrorsDetailsXml">false</xsl:param>
	<xsl:param name="originalMessage"/>
	<xsl:param name="serviceName"/>
	<xsl:param name="serviceContext"/>
	<xsl:param name="operationName"/>
	<xsl:param name="operationVersion">1</xsl:param>
	<xsl:param name="paradigm"/>
	<xsl:param name="fixResultNamespace">false</xsl:param>
	<!--
		if $errorCode is empty then
		 - the complete input message is copied
		 - a result tag is added as last child of the root tag if it doesn't exist and $paradigm equals 'Response'
		 - if the result tag exists and $fixResultNamespace equals true, the namespace of the result tag is changed to $namespace
		else
		 - the root tag of the input message is copied
		 - a result tag is wrapped in this copied root tag
		 - if $addErrorsDetailsXml equals true, $errorDetailsXml is not empty and the root of $errorDetailsXml equals
		    > 'errorMessage': this error is copied with Detail/Code=ERROR
		    > 'reasons': all errors are copied with Detail/Code=ERROR. Also $originalMessage will be copied as error with Detail/Code=ORIGINAL_MESSAGE
	-->
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
	<xsl:variable name="result_exists">
		<xsl:choose>
			<xsl:when test="*/*[local-name(.)='Result']">true</xsl:when>
			<xsl:otherwise>false</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<xsl:template match="/">
		<xsl:choose>
			<xsl:when test="string-length($errorCode)=0">
				<xsl:apply-templates select="*|comment()|processing-instruction()" mode="ok"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates select="*|comment()|processing-instruction()" mode="error"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="*|@*|comment()|processing-instruction()|text()" mode="ok">
		<xsl:choose>
			<xsl:when test="self::* and local-name(.)='Result' and $fixResultNamespace='true'">
				<xsl:element name="Result" namespace="{$ns}">
					<xsl:apply-templates select="*|@*|comment()|processing-instruction()|text()" mode="fixResultNamespace"/>
				</xsl:element>
			</xsl:when>
			<xsl:otherwise>
				<xsl:copy>
					<xsl:apply-templates select="*|@*|comment()|processing-instruction()|text()" mode="ok"/>
					<xsl:if test="not(parent::*) and $result_exists='false' and $paradigm='Response'">
						<xsl:call-template name="Result"/>
					</xsl:if>
				</xsl:copy>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="*|@*|comment()|processing-instruction()|text()" mode="fixResultNamespace">
		<xsl:choose>
			<xsl:when test="self::*">
				<xsl:element name="{local-name(.)}" namespace="{$ns}">
					<xsl:apply-templates select="*|@*|comment()|processing-instruction()|text()" mode="fixResultNamespace" />
				</xsl:element>
			</xsl:when>
			<xsl:otherwise>
				<xsl:copy>
					<xsl:apply-templates select="*|@*|comment()|processing-instruction()|text()" mode="fixResultNamespace" />
				</xsl:copy>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="*|@*|comment()|processing-instruction()|text()" mode="error">
		<xsl:if test="not(parent::*)">
			<xsl:copy>
				<xsl:apply-templates select="*|@*|comment()|processing-instruction()|text()" mode="error"/>
				<xsl:call-template name="Result"/>
			</xsl:copy>
		</xsl:if>
	</xsl:template>
	<xsl:template name="Result">
		<xsl:element name="Result" namespace="{$ns}">
			<xsl:choose>
				<xsl:when test="string-length($errorCode)=0">
					<xsl:element name="Status" namespace="{$ns}">OK</xsl:element>
				</xsl:when>
				<xsl:otherwise>
					<xsl:element name="Status" namespace="{$ns}">ERROR</xsl:element>
					<xsl:element name="ErrorList" namespace="{$ns}">
						<xsl:element name="Error" namespace="{$ns}">
							<xsl:element name="Code" namespace="{$ns}">
								<xsl:value-of select="$errorCode"/>
							</xsl:element>
							<xsl:element name="Reason" namespace="{$ns}">
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
										<xsl:value-of select="$errorReason"/>
										<xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:element>
							<xsl:element name="Service" namespace="{$ns}">
								<xsl:element name="Name" namespace="{$ns}">
									<xsl:value-of select="$serviceName"/>
								</xsl:element>
								<xsl:element name="Context" namespace="{$ns}">
									<xsl:value-of select="$serviceContext"/>
								</xsl:element>
								<xsl:element name="Action" namespace="{$ns}">
									<xsl:element name="Paradigm" namespace="{$ns}">
										<xsl:value-of select="$paradigm"/>
									</xsl:element>
									<xsl:element name="Name" namespace="{$ns}">
										<xsl:value-of select="$operationName"/>
									</xsl:element>
									<xsl:element name="Version" namespace="{$ns}">
										<xsl:value-of select="$operationVersion"/>
									</xsl:element>
								</xsl:element>
							</xsl:element>
							<xsl:choose>
								<xsl:when test="$addErrorsDetailsXml='true' and string-length($errorDetailsXml)&gt;0 and local-name($errorDetailsXml/*)='errorMessage'">
									<xsl:element name="DetailList" namespace="{$ns}">
										<xsl:element name="Detail" namespace="{$ns}">
											<xsl:element name="Code" namespace="{$ns}">
												<xsl:text>ERROR</xsl:text>
											</xsl:element>
											<xsl:element name="Text" namespace="{$ns}">
												<xsl:value-of select="$errorDetailsXml/errorMessage/@message"/>
											</xsl:element>
										</xsl:element>
									</xsl:element>
								</xsl:when>
								<xsl:when test="$addErrorsDetailsXml='true' and string-length($errorDetailsXml)&gt;0 and local-name($errorDetailsXml/*)='reasons'">
									<xsl:element name="DetailList" namespace="{$ns}">
										<xsl:if test="string-length($originalMessage)&gt;0">
											<xsl:element name="Detail" namespace="{$ns}">
												<xsl:element name="Code" namespace="{$ns}">
													<xsl:text>ORIGINAL_MESSAGE</xsl:text>
												</xsl:element>
												<xsl:element name="Text" namespace="{$ns}">
													<xsl:value-of select="normalize-space($originalMessage)"/>
												</xsl:element>
											</xsl:element>
										</xsl:if>
										<xsl:for-each select="$errorDetailsXml/reasons/reason">
											<xsl:element name="Detail" namespace="{$ns}">
												<xsl:element name="Code" namespace="{$ns}">
													<xsl:text>ERROR</xsl:text>
												</xsl:element>
												<xsl:element name="Text" namespace="{$ns}">
													<xsl:value-of select="string-join((xpath,location,message)[string-length(.)&gt;0],': ')"/>
												</xsl:element>
											</xsl:element>
										</xsl:for-each>
									</xsl:element>
								</xsl:when>
								<xsl:otherwise>
									<xsl:if test="string-length($errorDetailCode)&gt;0">
										<xsl:element name="DetailList" namespace="{$ns}">
											<xsl:element name="Detail" namespace="{$ns}">
												<xsl:element name="Code" namespace="{$ns}">
													<xsl:value-of select="$errorDetailCode"/>
												</xsl:element>
												<xsl:if test="string-length($errorDetailText)&gt;0">
													<xsl:element name="Text" namespace="{$ns}">
														<xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
														<xsl:value-of select="$errorDetailText"/>
														<xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
													</xsl:element>
												</xsl:if>
											</xsl:element>
										</xsl:element>
									</xsl:if>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:element>
					</xsl:element>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:element>
	</xsl:template>
</xsl:stylesheet>
