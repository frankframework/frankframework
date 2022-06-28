<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" cdata-section-elements="message" />
	<xsl:param name="fromaddress"/>
	<xsl:param name="hostname"/>
	<xsl:param name="message"/>
	<xsl:param name="mailaddresses"/>
	<xsl:param name="result" />
	<xsl:variable name="from" select="hostname"/>
	<xsl:variable name="to" select="tokenize($mailaddresses,',')[1]"/>
	<xsl:variable name="cc" select="substring-after($mailaddresses,',')"/>
	
	<xsl:template match="/">
		<email>
			<recipients>
				<recipient type="to"><xsl:value-of select="$to"/></recipient>
				<xsl:for-each select="tokenize($cc,',')">
					<recipient type="cc"><xsl:value-of select="."/></recipient>
				</xsl:for-each>
			</recipients>
			<from><xsl:value-of select="$from"/>&lt;<xsl:value-of select="$fromaddress"/>&gt;</from>
			<subject>
				<xsl:value-of select="$result"/>
				<xsl:value-of select="concat(' Larva run results on ', $hostname, ' (')"/>
				<xsl:value-of select="tokenize($message, '\n')[last() - 1]"/>
				<xsl:value-of select="')'"/>
			</subject>
			<!--
			CDATA will be added by cdata-section-elements="message" attribute
			on xsl:output. This will prevent the MailSender from removing
			whitespace at the beginning of the message.
			-->
			<message>
				<!--
				http://support.xink.io/support/solutions/articles/1000064098-why-is-outlook-stripping-line-breaks-from-plain-text-emails
				https://stackoverflow.com/questions/247546/outlook-autocleaning-my-line-breaks-and-screwing-up-my-email-format
				https://www.masternewmedia.org/newsletter_publishing/newsletter_formatting/remove_line_breaks_issue_Microsoft_Outlook_2003_when_publishing_text_newsletters_20051217.htm
				-->
				<xsl:value-of select="'  '"/>
				<xsl:call-template name="replace">
					<xsl:with-param name="string" select="$message"/>
					<xsl:with-param name="old" select="'&#10;'"/>
					<xsl:with-param name="new" select="'&#10;  '"/>
				</xsl:call-template>
			</message>
		</email>
	</xsl:template>

	<xsl:template name="replace">
		<xsl:param name="string"/>
		<xsl:param name="old"/>
		<xsl:param name="new"/>
		<xsl:choose>
			<xsl:when test="contains($string, $old)">
				<xsl:value-of select="concat(substring-before($string, $old), $new)"/>
				<xsl:call-template name="replace">
					<xsl:with-param name="string" select="substring-after($string, $old)"/>
					<xsl:with-param name="old" select="$old"/>
					<xsl:with-param name="new" select="$new"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$string"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

</xsl:stylesheet>
