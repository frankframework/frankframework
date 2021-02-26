<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>
	<xsl:strip-space elements="*"/>
	
	<xsl:template match="/Report">
		<xsl:copy>
			<!-- Select all report attributes -->
			<!-- <xsl:apply-templates select="@*"/> -->

			<!-- Select the report name attribute -->
			<!-- <xsl:apply-templates select="@Name"/> -->

			<!-- Select all report attributes except the name attribute -->
			<!-- <xsl:apply-templates select="@*[local-name() != 'Name']"/> -->

			<!-- For comparing reports it's better not to select (most of) the report attributes (and not enable the -->
			<!-- examples above) because (most of) the report attributes change on every rerun or can be adjusted by -->
			<!-- the user in between runs (e.g. when renaming a report) -->

			<!-- Select the first and last checkpoint -->
			<xsl:apply-templates select="Checkpoint[1]"/>
			<xsl:apply-templates select="Checkpoint[last()]"/>

			<!-- Select all checkpoints -->
			<!-- <xsl:apply-templates select="node()"/> -->

			<!-- Select the checkpoint with name "Pipe Example" -->
			<!-- <xsl:apply-templates select="Checkpoint[@Name='Pipe Example']"/> -->
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="node()|@*">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*"/>
		</xsl:copy>
	</xsl:template>
	
	<!-- Ignore content of timestamp element -->
	<!-- <xsl:template match="timestamp"><TIMESTAMP-IGNORED/></xsl:template> -->

	<!-- Ignore content of Timestamp element in xml messages with namespaces (e.g. in case of SOAP messages) -->
	<!-- <xsl:template match="*[local-name()='Timestamp']"><xsl:element name="TIMESTAMP-IGNORED" namespace="{namespace-uri()}"/></xsl:template> -->

	<!-- Ignore content of elements which content is ID:something -->
	<!-- <xsl:template match="*[matches(text(), 'ID:.*')]">ID:IGNORED</xsl:template> -->

</xsl:stylesheet>
