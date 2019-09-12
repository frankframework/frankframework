<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:str="http://exslt.org/strings" extension-element-prefixes="str" exclude-result-prefixes="str">
     <xsl:output method="text"/>
  <xsl:param name="fileName"/>
  <xsl:param name="afterDirWindows" ><xsl:if test="substring($fileName, string-length($fileName)) != '\\'"><xsl:value-of select="str:tokenize($fileName, '\\')[last()]" /></xsl:if></xsl:param>
  <xsl:param name="afterDirUnix" ><xsl:if test="substring($fileName, string-length($fileName)) != '/'"><xsl:value-of select="str:tokenize($fileName, '/')[last()]" /></xsl:if></xsl:param>  
   
  <xsl:template match="/">
    <xsl:choose>
      <xsl:when test="contains($fileName, '\')">
        <xsl:value-of select="$afterDirWindows"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:if test="contains($fileName,'/')">
        <xsl:value-of select="$afterDirUnix" />
        </xsl:if>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>   
   
 </xsl:stylesheet>