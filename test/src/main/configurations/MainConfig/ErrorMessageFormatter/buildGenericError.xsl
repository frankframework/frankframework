<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
	<xsl:param name="foutCode"/>
	<xsl:variable name="errorNode" select="document('ErrorList.xml')" />

	<xsl:template match="/">
		<profile>
			<entity name="kenmerken" instanceid="0" instancename="kenmerken_0">
				<attribute name="kenmerken.ALG_RTN_CD">1</attribute>
			</entity>
			<xsl:call-template name="fouten">
				<xsl:with-param name="id" select="'1'"/>
				<xsl:with-param name="foutCD" select="$foutCode"/>
			</xsl:call-template>
		</profile>
	</xsl:template>
	
	<xsl:template name="fouten">
		<xsl:param name="id"/>
		<xsl:param name="foutCD"/>
		
		<entity name="fouten" instanceid="{$id}" instancename="{concat('fouten_',$id)}">
			<attribute name="fouten.FOUT_CD"><xsl:value-of select="$foutCD"/></attribute>
			<attribute name="fouten.FOUT_OMSR_NL"><xsl:value-of select="$errorNode/Domains/Errors/Item[ErrorCode=$foutCD]/FOUT_OMSR_NL"/></attribute>
			<attribute name="fouten.FOUT_OMSR_EN"><xsl:value-of select="$errorNode/Domains/Errors/Item[ErrorCode=$foutCD]/FOUT_OMSR_EN"/></attribute>
			</entity>
	</xsl:template>
</xsl:stylesheet>