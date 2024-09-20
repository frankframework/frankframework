<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>
	<xsl:strip-space elements="*"/>

	<xsl:template match="/Report">

		<xsl:if test="Checkpoint[1]/*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='AuditLog_Action']"> 
			<xsl:text>&#10;</xsl:text><xsl:comment>#####################</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:comment>## AuditLog header ##</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:comment>#####################</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:apply-templates select="Checkpoint[1]/*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='AuditLog_Action']/*[local-name()='Header']"/>
		</xsl:if>

		<xsl:if test="Checkpoint[1]/*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='ExceptionLog_Action']"> 
			<xsl:text>&#10;</xsl:text><xsl:comment>#########################</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:comment>## ExceptionLog header ##</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:comment>#########################</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:apply-templates select="Checkpoint[1]/*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='ExceptionLog_Action']/*[local-name()='Header']"/>
		</xsl:if>

		<xsl:if test="Checkpoint[1]/*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='ExceptionLog_Action']/*[local-name()='Exception']">
			<xsl:text>&#10;</xsl:text><xsl:comment>###############</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:comment>## Exception ##</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:comment>###############</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:apply-templates select="Checkpoint[1]/*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='ExceptionLog_Action']/*[local-name()='Exception']"/>
		</xsl:if>

		<xsl:if test="Checkpoint[@Name = 'Column ERROR_CODE']">
			<xsl:text>&#10;</xsl:text><xsl:comment>################</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:comment>## Error code ##</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:comment>################</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:value-of
				select="Checkpoint[@Name = 'Column ERROR_CODE']"
				disable-output-escaping="yes"
			/>
		</xsl:if>

		<xsl:if test="Checkpoint[@Name = 'Column ERROR_REASON']">
			<xsl:text>&#10;</xsl:text><xsl:comment>##################</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:comment>## Error reason ##</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:comment>##################</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:value-of
				select="Checkpoint[@Name = 'Column ERROR_REASON']"
				disable-output-escaping="yes"
			/>
		</xsl:if>

		<xsl:if test="Checkpoint[@Name = 'Column ERROR_TEXT']">
			<xsl:text>&#10;</xsl:text><xsl:comment>################</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:comment>## Error text ##</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:comment>################</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:value-of
				select="Checkpoint[@Name = 'Column ERROR_TEXT']"
				disable-output-escaping="yes"
			/>
		</xsl:if>

		<xsl:text>&#10;</xsl:text><xsl:comment>#############</xsl:comment>
		<xsl:text>&#10;</xsl:text><xsl:comment>## Message ##</xsl:comment>
		<xsl:text>&#10;</xsl:text><xsl:comment>#############</xsl:comment>
		<xsl:choose>
			<xsl:when test="Checkpoint[@Name = 'Column MESSAGE']">
				<xsl:text>&#10;</xsl:text><xsl:apply-templates select="Checkpoint[@Name = 'Column MESSAGE']/*"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>&#10;</xsl:text><xsl:value-of
					select="Checkpoint[1]/*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='AuditLog_Action' or local-name()='ExceptionLog_Action']/*[local-name()='Message']"
					disable-output-escaping="yes"
				/>
			</xsl:otherwise>
		</xsl:choose>

		<xsl:if test="Checkpoint[1]/*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='AuditLog_Action']/*[local-name()='FunctionalError']">
			<xsl:text>&#10;</xsl:text><xsl:comment>#####################</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:comment>## FunctionalError ##</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:comment>#####################</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:apply-templates select="Checkpoint[1]/*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='AuditLog_Action']/*[local-name()='FunctionalError']"/>
		</xsl:if>

		<xsl:if test="Checkpoint[@Name = 'Column FUNCT_ERROR_CODE']">
			<xsl:text>&#10;</xsl:text><xsl:comment>###########################</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:comment>## Functional error code ##</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:comment>###########################</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:value-of
				select="Checkpoint[@Name = 'Column FUNCT_ERROR_CODE']"
				disable-output-escaping="yes"
			/>
		</xsl:if>

		<xsl:if test="Checkpoint[@Name = 'Column FUNCT_ERROR_REASON']">
			<xsl:text>&#10;</xsl:text><xsl:comment>#############################</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:comment>## Functional error reason ##</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:comment>#############################</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:value-of
				select="Checkpoint[@Name = 'Column FUNCT_ERROR_REASON']"
				disable-output-escaping="yes"
			/>
		</xsl:if>

		<xsl:if test="Checkpoint[@Name = 'Column FUNCT_ERROR_TEXT']">
			<xsl:text>&#10;</xsl:text><xsl:comment>###########################</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:comment>## Functional error text ##</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:comment>###########################</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:value-of
				select="Checkpoint[@Name = 'Column FUNCT_ERROR_TEXT']"
				disable-output-escaping="yes"
			/>
		</xsl:if>

		<xsl:if test="Checkpoint[1]/*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='ExceptionLog_Action']/*[local-name()='Message']/*[local-name()='Resend']">
			<xsl:text>&#10;</xsl:text><xsl:comment>############</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:comment>## Resend ##</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:comment>############</xsl:comment>
			<xsl:text>&#10;</xsl:text><xsl:apply-templates select="Checkpoint[1]/*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='ExceptionLog_Action']/*[local-name()='Message']/*[local-name()='Resend']"/>
		</xsl:if>

	</xsl:template>

	<xsl:template match="node()|@*">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*"/>
		</xsl:copy>
	</xsl:template>

</xsl:stylesheet>
