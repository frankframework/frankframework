<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>
	<xsl:strip-space elements="*"/>
	<xsl:template match="/Report">
		<xsl:copy>
			<xsl:apply-templates select="@Name"/>
			<!--
			To select all original report attributes:
			<xsl:apply-templates select="@*"/>
			-->
			<!--incoming message>-->
			<xsl:text>&#10;</xsl:text>
			<xsl:comment>#############################</xsl:comment>
			<xsl:text>&#10;</xsl:text>
			<xsl:comment>## (1) Message received ##</xsl:comment>
			<xsl:text>&#10;</xsl:text>
			<xsl:comment>#############################</xsl:comment>
			<xsl:text>&#10;</xsl:text>
			<xsl:apply-templates select="Checkpoint[1]"/>
			<!-- messages sent to MSG -->
			<xsl:text>&#10;</xsl:text>
			<xsl:comment>#############################</xsl:comment>
			<xsl:text>&#10;</xsl:text>
			<xsl:comment>## (1) Messages sent to MSG ##</xsl:comment>
			<xsl:text>&#10;</xsl:text>
			<xsl:comment>#############################</xsl:comment>
			<xsl:text>&#10;</xsl:text>
			<xsl:apply-templates select="Checkpoint[starts-with(@Name, 'Sender JMS Sender for IFSACall1') and @Type='Startpoint']"/>
			<xsl:apply-templates select="Checkpoint[starts-with(@Name, 'Sender JMS Sender for MQ') and @Type='Startpoint']"/>			
			<!-- messages received from MSG -->
			<xsl:text>&#10;</xsl:text>
			<xsl:comment>#############################</xsl:comment>
			<xsl:text>&#10;</xsl:text>
			<xsl:comment>## (1) Messages received from MSG ##</xsl:comment>
			<xsl:text>&#10;</xsl:text>
			<xsl:comment>#############################</xsl:comment>
			<xsl:text>&#10;</xsl:text>
			<xsl:apply-templates select="Checkpoint[starts-with(@Name, 'Sender JMS Sender for IFSACall1') and @Type='Endpoint']"/>
			<xsl:apply-templates select="Checkpoint[starts-with(@Name, 'Sender JMS Sender for MQ') and @Type='Endpoint']"/>
					<!-- messages sent to MSG to retrieve projected cost compensation -->
			<xsl:text>&#10;</xsl:text>
			<xsl:comment>#############################</xsl:comment>
			<xsl:text>&#10;</xsl:text>
			<xsl:comment>## (1) Projected cost compensation request sent to MSG ##</xsl:comment>
			<xsl:text>&#10;</xsl:text>
			<xsl:comment>#############################</xsl:comment>
			<xsl:text>&#10;</xsl:text>
			<xsl:apply-templates select="Checkpoint[starts-with(@Name, 'Sender JMS Sender for IFSACall2') and @Type='Startpoint']"/>
			<!-- messages received from MSG with projected cost compensation -->
			<xsl:text>&#10;</xsl:text>
			<xsl:comment>#############################</xsl:comment>
			<xsl:text>&#10;</xsl:text>
			<xsl:comment>## (1) Projected cost compensation response received from MSG ##</xsl:comment>
			<xsl:text>&#10;</xsl:text>
			<xsl:comment>#############################</xsl:comment>
			<xsl:text>&#10;</xsl:text>
			<xsl:apply-templates select="Checkpoint[starts-with(@Name, 'Sender JMS Sender for IFSACall2') and @Type='Endpoint']"/>
			<!-- message returned -->
			<xsl:text>&#10;</xsl:text>
			<xsl:comment>#############################</xsl:comment>
			<xsl:text>&#10;</xsl:text>
			<xsl:comment>## (6) Message returned ##</xsl:comment>
			<xsl:text>&#10;</xsl:text>
			<xsl:comment>#############################</xsl:comment>
			<xsl:text>&#10;</xsl:text>
			<xsl:apply-templates select="Checkpoint[last()]"/>
			<!--
			To select all nodes use the following instead of the previous two lines:
			<xsl:apply-templates select="node()"/>
			-->
		</xsl:copy>
	</xsl:template>
	<xsl:template match="node()|@*">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*"/>
		</xsl:copy>
	</xsl:template>
	<!--
	Ignore content of timestamp tag:
	<xsl:template match="timestamp">
		<TIMESTAMP-IGNORED/>
	</xsl:template>
	 -->
</xsl:stylesheet>
