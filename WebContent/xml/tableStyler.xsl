<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">

	
	
	<xsl:template match="contentTable">
		<table>
		<!-- process a TABLE element. 
		      First process the CAPTION element if present, transforming it to a TabHeader,
		      Then processing subsequent TR or TBODY elements
		 -->
		<xsl:variable name="theNode"><xsl:value-of select="TBODY"/></xsl:variable>

		<!-- <xsl:call-template name="panel">
			<xsl:with-param name="title"><xsl:value-of select="CAPTION"/></xsl:with-param>
			<xsl:with-param name="body"><xsl:value-of select="TBODY"/></xsl:with-param>
		</xsl:call-template>
		-->
			<xsl:apply-templates/>
		</table>
	</xsl:template>
	
	<!-- if a tr with parameter alternatingRows=true then alternating rows get
	     alternating colors 
	-->
	<xsl:template match="contentTable/caption">
		<xsl:copy>
			<xsl:attribute name="class">caption</xsl:attribute>
			<xsl:apply-templates select="text()|./*"/>
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="contentTable/tbody">
		<xsl:for-each select="tr">
			<xsl:element name="tr">

			<xsl:choose>
			<xsl:when test="@alternatingRows='true'">
				<xsl:attribute name="class">
					<xsl:choose>
							<xsl:when test="(position() mod 2) = 1">rowEven</xsl:when>
							<xsl:otherwise>filterRow</xsl:otherwise>
					</xsl:choose>
				</xsl:attribute>
			</xsl:when>
			<xsl:when test="@ref='adapterRow'">
				<xsl:attribute name="class">adapterRow</xsl:attribute>
			</xsl:when>
			<xsl:when test="@ref='receiverRow'">
				<xsl:attribute name="class">receiverRow</xsl:attribute>
			</xsl:when>
			<xsl:when test="@ref='messagesRow'">
				<xsl:attribute name="class">messagesRow</xsl:attribute>
			</xsl:when>
			<xsl:when test="@ref='spannedRow'">
				<xsl:attribute name="valign">top</xsl:attribute>
			</xsl:when>
			</xsl:choose>

			<xsl:apply-templates select="./*"/>
			
			</xsl:element>
		</xsl:for-each>
	</xsl:template>

	<xsl:template match="contentTable/tbody/tr/subHeader">
		<td class="subHeader">
			<xsl:apply-templates select="@*|text()|./*"/>
		</td>
	</xsl:template>

	<xsl:template match="contentTable/tbody/tr/td">
		<xsl:element name="td">
		<xsl:apply-templates select="@*"/>
			<xsl:if test="not(@class)">
				<xsl:attribute name="class">filterRow</xsl:attribute>
			</xsl:if>
			<xsl:apply-templates select="./*|text()"/>
		</xsl:element>
	</xsl:template>

	<xsl:template match="contentTable/tbody/tr/th" >
	
				<xsl:element name="th">
					<xsl:copy-of select="@*"/>
					<xsl:attribute name="class">colHeader</xsl:attribute>
							<xsl:apply-templates select="text()|*"/>
				</xsl:element>
	</xsl:template>


</xsl:stylesheet>
