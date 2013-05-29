<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes"/>
	<xsl:template match="/">
		<attributes>
			<xsl:for-each select="//@*">
				<attribute>
					<key>
						<xsl:value-of select="name()"/>
					</key>
					<value>
						<xsl:value-of select="."/>
					</value>
					<element>
						<xsl:value-of select="name(parent::*)"/>
					</element>
					<name>
						<xsl:value-of select="parent::*/@name"/>
					</name>
				</attribute>
			</xsl:for-each>
		</attributes>
	</xsl:template>
</xsl:stylesheet>
