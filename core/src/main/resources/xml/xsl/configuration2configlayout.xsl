<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" version="2.0">
	<xsl:output method="text" indent="yes" omit-xml-declaration="yes"/>

	<xsl:param name="frankElements"/>
	<xsl:variable name="errorForwards" select="('exception','failure','fail','timeout','illegalResult','presumedTimeout','interrupt','parserError','outputParserError','outputFailure')"/>

	<xsl:template match="/">
		<xsl:variable name="preproccessedConfiguration">
			<xsl:apply-templates select="*" mode="preprocess"/>
		</xsl:variable>

		<xsl:text>flowchart&#10;</xsl:text>
		<xsl:apply-templates select="$preproccessedConfiguration" mode="convertElements"/>
		<xsl:apply-templates select="$preproccessedConfiguration//forward" mode="convertForwards"/>
	</xsl:template>

	<xsl:template match="*" mode="preprocess">
		<xsl:copy>
			<xsl:call-template name="defaultCopyActions"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="@*|comment()" mode="preprocess">
		<xsl:copy/>
	</xsl:template>

	<xsl:template match="forward" mode="preprocess"/>

	<xsl:template match="adapter" mode="preprocess">
		<xsl:copy>
			<xsl:copy-of select="@*"/>
			<xsl:variable name="forward">
				<forward name="" path="{generate-id(pipeline)}" targetID="{generate-id(pipeline)}"/>
			</xsl:variable>
			<xsl:apply-templates select="*" mode="#current">
				<xsl:with-param name="forward" select="$forward" tunnel="yes"/>
			</xsl:apply-templates>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="listener" mode="preprocess">
		<xsl:param name="forward" tunnel="yes"/>
		<xsl:copy>
			<xsl:call-template name="defaultCopyActions"/>
			<xsl:copy-of select="$forward"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="pipeline" mode="preprocess">
		<xsl:copy>
			<xsl:call-template name="defaultCopyActions"/>
			<xsl:for-each select="pipe//sender[not(exists(sender))]">
				<forward name="" path="{generate-id()}" targetID="{generate-id()}"/>
			</xsl:for-each>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="pipe" mode="preprocess">
		<xsl:copy>
			<xsl:apply-templates select=".//sender[not(exists(sender))]" mode="#current"/>
		</xsl:copy>
	</xsl:template>

	<!-- For local senders, add a 'forward' to the sub-adapter -->
	<xsl:template match="sender[@className='org.frankframework.senders.IbisLocalSender']" mode="preprocess">
		<xsl:copy>
			<xsl:call-template name="defaultCopyActions"/>
			<xsl:variable name="targetListener" select="ancestor::adapter/../adapter/receiver/listener[@className='org.frankframework.receivers.JavaListener' and @name=current()/@javaListener]"/>
			<xsl:if test="exists($targetListener)">
				<forward name="" path="{$targetListener/@name}" targetID="{generate-id($targetListener)}"/>
			</xsl:if>
		</xsl:copy>
	</xsl:template>

	<xsl:template name="defaultCopyActions">
		<xsl:attribute name="elementID" select="generate-id()"/>
		<xsl:apply-templates select="@*|*" mode="#current"/>
		<xsl:copy-of select="$frankElements/(*[name()=current()/@className],*[name()=current()/name()])[1]/*"/>
	</xsl:template>

	<xsl:template match="*" mode="convertElements">
		<xsl:apply-templates select="*" mode="#current"/>
	</xsl:template>

	<xsl:template match="pipeline" mode="convertElements">
		<xsl:call-template name="createMermaidElement">
			<xsl:with-param name="text" select="parent::adapter/@name"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="listener" mode="convertElements">
		<xsl:call-template name="createMermaidElement">
			<xsl:with-param name="text" select="(tokenize(@className,'\.')[last()],'Listener')[1]"/>
			<xsl:with-param name="style" select="concat('listener', modifier)"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="sender" mode="convertElements">
		<xsl:call-template name="createMermaidElement">
			<xsl:with-param name="text" select="(tokenize(@className,'\.')[last()],'Sender')[1]"/>
			<xsl:with-param name="style" select="concat('sender', modifier)"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:variable name="shapeStartMap">
		<field type="listener"><![CDATA[>]]></field>
		<field type="endpoint">(</field>
	</xsl:variable>
	<xsl:variable name="shapeEndMap">
		<field type="listener">]</field>
		<field type="endpoint">)</field>
	</xsl:variable>
	<xsl:template name="createMermaidElement">
		<xsl:param name="text" select="xs:string((@name,name())[1])"/>
		<xsl:param name="style" select="'normal'"/>

		<xsl:text>  </xsl:text>
		<xsl:value-of select="@elementID"/>
		<xsl:value-of select="($shapeStartMap/field[@type = current()/type],'(')[1]"/>
		<xsl:text>'<![CDATA[<text]]></xsl:text>
		<xsl:if test="name() = 'pipeline'">
			<xsl:text> data-html-node="a"</xsl:text>
		</xsl:if>
		<xsl:text><![CDATA[>]]></xsl:text>
		<xsl:value-of select="$text"/>
		<xsl:text><![CDATA[</text>]]>'</xsl:text>
		<xsl:value-of select="($shapeEndMap/field[@type = current()/type],')')[1]"/>
		<xsl:text>:::</xsl:text>
		<xsl:value-of select="$style"/>
		<xsl:text>&#10;</xsl:text>
		<xsl:apply-templates select="*" mode="#current"/>
	</xsl:template>

	<xsl:template match="forward" mode="convertForwards">
		<xsl:text>  </xsl:text>
		<xsl:value-of select="parent::*/@elementID"/>
		<xsl:text> --> </xsl:text>
		<xsl:value-of select="@targetID"/>
		<xsl:text>&#10;</xsl:text>
	</xsl:template>
</xsl:stylesheet>
