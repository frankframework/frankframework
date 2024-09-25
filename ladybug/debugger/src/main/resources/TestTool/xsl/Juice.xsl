<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
  <xsl:output method="xml" omit-xml-declaration="yes"/>
  
  <xsl:template match="/Report">
    <xsl:copy>
      <xsl:apply-templates select="@Name"/>

      <xsl:text>&#10;</xsl:text>
      <xsl:comment>#############################</xsl:comment>
      <xsl:text>&#10;</xsl:text>
      <xsl:comment>## (1) Message received ##</xsl:comment>
      <xsl:text>&#10;</xsl:text>
      <xsl:comment>#############################</xsl:comment>
      <xsl:text>&#10;</xsl:text>

      <xsl:apply-templates select="child::Checkpoint[1]"/>

      <xsl:text>&#10;</xsl:text>
      <xsl:comment>###################################</xsl:comment>
      <xsl:text>&#10;</xsl:text>
      <xsl:comment>## (2) Message sent to IFSA ##</xsl:comment>
      <xsl:text>&#10;</xsl:text>
      <xsl:comment>###################################</xsl:comment>
      <xsl:text>&#10;</xsl:text>

      <xsl:apply-templates select="Checkpoint[starts-with(@Name, 'Sender call') and @Type='Startpoint']"/>
      <xsl:apply-templates select="Checkpoint[starts-with(@Name, 'Sender Call') and @Type='Startpoint']"/>
      <xsl:apply-templates select="Checkpoint[starts-with(@Name, 'Sender Get') and @Type='Startpoint']"/>
      <xsl:apply-templates select="Checkpoint[starts-with(@Name, 'Sender send') and @Type='Startpoint']"/>
      <xsl:apply-templates select="Checkpoint[starts-with(@Name, 'Sender Send') and @Type='Startpoint']"/>
      <xsl:apply-templates select="Checkpoint[starts-with(@Name, 'Sender ING_') and @Type='Startpoint']"/>
      <xsl:apply-templates select="Checkpoint[starts-with(@Name, 'Sender UNISYS_') and @Type='Startpoint']"/>
      <xsl:apply-templates select="Checkpoint[starts-with(@Name, 'Sender Handle') and @Type='Startpoint']"/>

      <xsl:text>&#10;</xsl:text>
      <xsl:comment>############################</xsl:comment>
      <xsl:text>&#10;</xsl:text>
      <xsl:comment>## (3) MSG adapter to MSG ##</xsl:comment>
      <xsl:text>&#10;</xsl:text>
      <xsl:comment>############################</xsl:comment>
      <xsl:text>&#10;</xsl:text>

      <xsl:apply-templates select="Checkpoint[@Name='Pipe RetrieveMsgFileIn' and (@Type='Endpoint' or @Type='Abortpoint')]"/>

      <xsl:text>&#10;</xsl:text>
      <xsl:comment>############################</xsl:comment>
      <xsl:text>&#10;</xsl:text>
      <xsl:comment>## (4) MSG to MSG adapter ##</xsl:comment>
      <xsl:text>&#10;</xsl:text>
      <xsl:comment>############################</xsl:comment>
      <xsl:text>&#10;</xsl:text>

      <xsl:apply-templates select="Checkpoint[@Name='Pipe RetrieveMsgFileOut' and (@Type='Endpoint' or @Type='Abortpoint')]"/>

      <xsl:text>&#10;</xsl:text>
      <xsl:comment>###################################</xsl:comment>
      <xsl:text>&#10;</xsl:text>
      <xsl:comment>## (5) Message received from IFSA ##</xsl:comment>
      <xsl:text>&#10;</xsl:text>
      <xsl:comment>###################################</xsl:comment>
      <xsl:text>&#10;</xsl:text>

	 <xsl:apply-templates select="Checkpoint[starts-with(@Name, 'Sender call') and @Type='Endpoint']"/>
      <xsl:apply-templates select="Checkpoint[starts-with(@Name, 'Sender Call') and @Type='Endpoint']"/>
      <xsl:apply-templates select="Checkpoint[starts-with(@Name, 'Sender Get') and @Type='Endpoint']"/>
      <xsl:apply-templates select="Checkpoint[starts-with(@Name, 'Sender send') and @Type='Endpoint']"/>
      <xsl:apply-templates select="Checkpoint[starts-with(@Name, 'Sender Send') and @Type='Endpoint']"/>
      <xsl:apply-templates select="Checkpoint[starts-with(@Name, 'Sender ING_') and @Type='Endpoint']"/>
      <xsl:apply-templates select="Checkpoint[starts-with(@Name, 'Sender UNISYS_') and @Type='Endpoint']"/>
      <xsl:apply-templates select="Checkpoint[starts-with(@Name, 'Sender Handle') and @Type='Endpoint']"/>
	 
      <xsl:text>&#10;</xsl:text>
      <xsl:comment>#############################</xsl:comment>
      <xsl:text>&#10;</xsl:text>
      <xsl:comment>## (6) Message returned ##</xsl:comment>
      <xsl:text>&#10;</xsl:text>
      <xsl:comment>#############################</xsl:comment>
      <xsl:text>&#10;</xsl:text>

      <xsl:apply-templates select="Checkpoint[last()]"/>

      <xsl:text>&#10;</xsl:text>
      <xsl:comment>#############################</xsl:comment>
      <xsl:text>&#10;</xsl:text>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>