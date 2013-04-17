<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<xsl:key name="x" match="//@*[starts-with(name(),'authAlias') or ends-with(name(),'AuthAlias')]" use="." />
	<xsl:template match="/">
		<authEntries>
			<xsl:for-each select="//@*[starts-with(name(),'authAlias') or ends-with(name(),'AuthAlias')]">
				<xsl:sort select="." order="ascending" />
				<xsl:if test="generate-id(.)=generate-id(key('x',.)[1])">
					<entry>
						<xsl:attribute name="alias">
							<xsl:value-of select="." />
						</xsl:attribute>
					</entry>
				</xsl:if>
			</xsl:for-each>
		</authEntries>
	</xsl:template>
</xsl:stylesheet>
