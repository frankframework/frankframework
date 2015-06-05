<!-- key die in stylesdoc zoekt op xstyle -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 	>

	<xsl:key name="blockkey" match="//block" use="@name"/>
	<xsl:variable name="blocksdoc" select="document('blocks.xml')"/>

	<xsl:template match="pagefragment">
	<xsl:choose>
	       <xsl:when test="count(@idref)>0">
		     <xsl:call-template name="handlepagefragment">
			  <xsl:with-param name="blockid" select="@idref"/>
		     </xsl:call-template>
	       </xsl:when>
	       <xsl:otherwise>
		   <div style="fragment">
			 <xsl:apply-templates select="*"/>
		   </div> 
	       </xsl:otherwise>
	</xsl:choose>
	</xsl:template>
	
	<xsl:template name="handlepagefragment">
	<xsl:param name="blockid"/>
	   <xsl:for-each select="document('blocks.xml')">
			<xsl:for-each select="key('blockkey', $blockid)">
			      <xsl:apply-templates select="pagefragment"/>
		 </xsl:for-each> 
	   </xsl:for-each>
	</xsl:template>
</xsl:stylesheet>