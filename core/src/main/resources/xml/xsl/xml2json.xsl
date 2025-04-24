<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" version="2.0">
	<xsl:output method="text" media-type="application/json" omit-xml-declaration="yes"/>
	<xsl:param name="includeRootElement" as="xs:boolean"/>
	<xsl:template match="/">
		<xsl:choose>
			<xsl:when test="$includeRootElement">
				<xsl:call-template name="node">
					<xsl:with-param name="originalMessage">
						<xsl:copy-of select="."/>
					</xsl:with-param>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:for-each select="*">
					<xsl:call-template name="nodeValue"/>
				</xsl:for-each>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template name="node">
		<xsl:param name="originalMessage"/>
		<xsl:variable name="nodeNames">
			<nodeNames>
				<xsl:for-each-group select="$originalMessage/*" group-by="name()">
					<nodeName>
						<xsl:value-of select="local-name()"/>
					</nodeName>
				</xsl:for-each-group>
			</nodeNames>
		</xsl:variable>
		<xsl:variable name="countDistinctNodes" select="count($nodeNames/nodeNames/nodeName)"/>
		<xsl:value-of select="'{'"/>
		<xsl:for-each select="$nodeNames/nodeNames/nodeName">
			<xsl:variable name="nodeName" select="."/>
			<xsl:variable name="countNodes" select="count($originalMessage/*[local-name()=$nodeName])"/>
			<name>
				<xsl:value-of select="'&quot;'"/>
				<xsl:value-of select="$nodeName"/>
				<xsl:value-of select="'&quot;:'"/>
			</name>
			<value>
				<xsl:choose>
					<xsl:when test="$countNodes=1">
						<xsl:for-each select="$originalMessage/*[local-name()=$nodeName]">
							<xsl:call-template name="nodeValue"/>
						</xsl:for-each>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="'['"/>
						<xsl:for-each select="$originalMessage/*[local-name()=$nodeName]">
							<xsl:call-template name="nodeValue"/>
							<xsl:if test="position()!=$countNodes">
								<xsl:value-of select="','"/>
							</xsl:if>
						</xsl:for-each>
						<xsl:value-of select="']'"/>
					</xsl:otherwise>
				</xsl:choose>
			</value>
			<xsl:if test="position()!=$countDistinctNodes">
				<xsl:value-of select="','"/>
			</xsl:if>
		</xsl:for-each>
		<xsl:value-of select="'}'"/>
	</xsl:template>
	<xsl:template name="nodeValue">
		<xsl:choose>
			<xsl:when test="@nil='true'" >null</xsl:when>
			<xsl:when test="count(./*)=0">
				<xsl:value-of select="'&quot;'"/>
				<xsl:variable name="message">
					<xsl:for-each select="tokenize(.,'&quot;')">
						<xsl:if test="position()!=1">
							<xsl:value-of select="'&amp;quot;'"/>
						</xsl:if>
						<xsl:copy-of select="normalize-space(.)"/>
					</xsl:for-each>
				</xsl:variable>
				<xsl:for-each select="tokenize($message,'\\')">
					<xsl:if test="position()!=1">
						<xsl:value-of select="'&amp;#92;'"/>
					</xsl:if>
					<xsl:copy-of select="."/>
				</xsl:for-each>
				<xsl:value-of select="'&quot;'"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="node">
					<xsl:with-param name="originalMessage">
							<xsl:copy-of select="*/."/>
					</xsl:with-param>
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
