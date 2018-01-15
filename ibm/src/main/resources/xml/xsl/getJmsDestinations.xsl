<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>
	<xsl:template match="/">
		<destinations>
			<xsl:for-each-group select="XMI/*[name()='JMSProvider']" group-by="factories[ends-with(@type,'JMSConnectionFactory')]/@jndiName">
				<xsl:sort select="current-grouping-key()"/>
				<connectionFactory>
					<xsl:attribute name="jndiName"><xsl:value-of select="current-grouping-key()"></xsl:value-of></xsl:attribute>
					<xsl:for-each select="factories[ends-with(@type,'JMSDestination')]">
						<xsl:sort select="@jndiName"/>
						<destination>
							<xsl:attribute name="jndiName"><xsl:value-of select="@jndiName"/></xsl:attribute>
							<xsl:value-of select="@externalJNDIName"/>
						</destination>
					</xsl:for-each>
				</connectionFactory>
			</xsl:for-each-group>
		</destinations>
	</xsl:template>
</xsl:stylesheet>
