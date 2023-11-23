<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:java="http://xml.apache.org/xslt/java"
	xmlns:func="http://exslt.org/functions"
	xmlns:ibisext="http://frankframework.org/ibisext"
	xmlns:cal="xalan://java.util.Calendar"
	exclude-result-prefixes="java func ibisext cal" version="1.0">
	
	<xsl:param name="zero" select="0"/>
	
	<!-- $Id: genericFunctions.xsl,v 1.1 2009/10/08 12:22:34 m01e194 Exp $ -->
	<!-- 
		$Log: genericFunctions.xsl,v $
		Revision 1.1  2009/10/08 12:22:34  m01e194
		log.level aangepast (alleen DEBUG)
		XSLT transformatie aangevuld.
		
		Revision 1.1  2009/07/22 11:30:59  m01e194
		Added configuration and tescenario for replacing Quotes in SQL statements.
		
		Revision 1.3  2007/02/19 08:18:37  europe\m01e194
		*** empty log message ***
		
		Revision 1.2  2007/02/07 15:09:14  europe\m01e194
		Oude template convertDate als deprecated gemarkeerd.
		
		Revision 1.7  2007/02/07 15:08:55  europe\m01e194
		Oude template convertDate als deprecated gemarkeerd.
		
		Revision 1.6  2007/02/07 14:18:59  europe\m01e194
		convertDate als functie toegevoegd.
			Include genericFunctions in de XSL
			en neem de namespace op in <xsl:stylesheet>:
				xmlns:ibisext="http://frankframework.org/ibisext"
		
		Zie voor voorbeeld example.xsl
		
		Revision 1.5  2007/02/07 12:48:41  europe\m01e194
		*** empty log message ***
		
		Revision 1.4  2007/02/07 12:44:11  europe\m01e194
		Log header toegevoegd.
	-->
	
	<func:function name="ibisext:changeDate">
		<xsl:param name="date"/>
		<xsl:param name="years" select="0"/>
		<xsl:param name="months" select="0"/>
		<xsl:param name="days" select="0"/>
		<xsl:param name="dateFormat" select="'yyyy-MM-dd'" />
	
		<xsl:variable name="result">
			<xsl:if test="$date != ''">
				<xsl:variable name="df" select="java:java.text.SimpleDateFormat.new($dateFormat)"/>
			    <xsl:variable name="tmpDate" select="java:parse($df, string($date))"/>
			    <xsl:variable name="testDate" select="java:format($df, $tmpDate)"/>
					        
				<xsl:if test="$testDate = $date">
				    <xsl:variable name="newcal" select="cal:getInstance()"/>
				    <xsl:variable name="newcal2" select="cal:setTime($newcal, $tmpDate)"/>
				    <xsl:variable name="newcal3" select="cal:add($newcal, 1, $years)"/>
				    <xsl:variable name="newcal4" select="cal:add($newcal, 2, $months)"/>
				    <xsl:variable name="newcal5" select="cal:add($newcal, 5, $days)"/>
			    
				    <xsl:value-of select="java:format($df, cal:getTime($newcal))" />
				</xsl:if>
			</xsl:if>
		</xsl:variable>

		<func:result select="$result"/>
	</func:function>

	<func:function name="ibisext:convertDate">
		<xsl:param name="from" />
		<xsl:param name="to" />
		<xsl:param name="value" />
		<xsl:param name="valueIfEmpty" select="''" />
		<xsl:param name="valueOnError" select="$valueIfEmpty" />
		<xsl:param name="replaceValue" select="''" />
		
		<xsl:variable name="strValue" select="string($value)"/>
		
		<xsl:variable name="result">
			<xsl:choose>
				<xsl:when test="$strValue = string($replaceValue)">
					<xsl:value-of select="$valueIfEmpty" />
				</xsl:when>	
				<xsl:when test="$strValue != ''">
				    <xsl:variable name="formatterFrom"       
				        select="java:java.text.SimpleDateFormat.new($from)"/>
				    <xsl:variable name="formatterTo"       
				        select="java:java.text.SimpleDateFormat.new($to)"/>        
				
				    <xsl:variable name="date" 
				        select="java:parse($formatterFrom, $strValue)"/>
				        
				    <xsl:variable name="tempValue" 
				        select="java:format($formatterFrom, $date)"/>
				    
				    <xsl:choose>
					    <xsl:when test="$strValue = $tempValue">
						    <xsl:value-of select="java:format($formatterTo, $date)" />
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="string($valueOnError)" />
						</xsl:otherwise>
					</xsl:choose>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="string($valueIfEmpty)" />
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<func:result select="$result"/>
	</func:function>
	
	<func:function name="ibisext:replaceCharsInString">
		  <xsl:param name="stringIn"/>
		  <xsl:param name="charsIn" select="&quot;'&quot;"/>
		  <xsl:param name="charsOut" select="&quot;''&quot;"/>
		  <xsl:choose>
		    <xsl:when test="contains($stringIn,$charsIn)">
		      <func:result select="concat(concat(substring-before($stringIn,$charsIn),$charsOut), ibisext:replaceCharsInString(substring-after($stringIn,$charsIn),$charsIn,$charsOut))"/>
		    </xsl:when>
		    <xsl:otherwise>
		      <func:result select="$stringIn"/>
		    </xsl:otherwise>
		  </xsl:choose>
	</func:function>
	
	<!-- template that does char replacement -->
	<xsl:template name="replaceCharsInString">
	  <xsl:param name="stringIn"/>
	  <xsl:param name="charsIn"/>
	  <xsl:param name="charsOut"/>
	  
	  <xsl:variable name="result">
	  <xsl:choose>
	    <xsl:when test="contains($stringIn,$charsIn)">
	      <xsl:value-of select="concat(substring-before($stringIn,$charsIn),$charsOut)"/>
	      <xsl:call-template name="replaceCharsInString">
	        <xsl:with-param name="stringIn" select="substring-after($stringIn,$charsIn)"/>
	        <xsl:with-param name="charsIn" select="$charsIn"/>
	        <xsl:with-param name="charsOut" select="$charsOut"/>
	      </xsl:call-template>
	    </xsl:when>
	    <xsl:otherwise>
	      <xsl:value-of select="$stringIn"/>
	    </xsl:otherwise>
	  </xsl:choose>
	  </xsl:variable>
	</xsl:template>
</xsl:stylesheet>