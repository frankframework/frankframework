<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<xsl:param name="srcPrefix" />
	<xsl:param name="configurationName" />
	<xsl:param name="classLoaderType" />
	<xsl:variable name="original" select="root/configSource/@original" />
	<xsl:template match="/">
		<page title="Show configuration">
			<xsl:attribute name="title"><xsl:value-of select="concat('Show configuration: ',$configurationName)" /><xsl:if test="string-length($classLoaderType)&gt;0"><xsl:value-of select="concat(' (',$classLoaderType,')')" /></xsl:if></xsl:attribute>
			<xsl:if test="name(*)='error'">
				<font color="red">
					<xsl:value-of select="*" />
				</font>
			</xsl:if>
			<xsl:call-template name="tabs" />
			<div class="tabpanel">
			<br/>
				<ul class="tab">
					<xsl:choose>
						<xsl:when test="$original='true'">
							<li id="showconfig">
								<a>
									<xsl:attribute name="href" select="concat($srcPrefix,'configHandler.do?action=showloadedconfig')" />
									<xsl:attribute name="alt" select="'showloadedconfig'" />
									<xsl:attribute name="text" select="'showloadedconfig'" />
									<xsl:value-of select="'showloadedconfig'" />
								</a>
							</li>
							<li class="active" id="showconfig">showoriginalconfig</li>
						</xsl:when>
						<xsl:otherwise>
							<li class="active" id="showconfig">showloadedconfig</li>
							<li>
								<a>
									<xsl:attribute name="href" select="concat($srcPrefix,'configHandler.do?action=showoriginalconfig')" />
									<xsl:attribute name="alt" select="'showoriginalconfig'" />
									<xsl:attribute name="text" select="'showoriginalconfig'" />
									<xsl:value-of select="'showoriginalconfig'" />
								</a>
							</li>
						</xsl:otherwise>
					</xsl:choose>
				</ul>
				<pre>
					<xsl:value-of select="root/configSource"/>
				</pre>
			</div>
		</page>
	</xsl:template>
	<xsl:template name="tabs">
		<xsl:if test="count(root/configurations/configuration)&gt;1">
			<ul class="tab">
				<xsl:for-each select="root/configurations/configuration">
					<xsl:sort select="@nameUC" />
					<xsl:choose>
						<xsl:when test=".=$configurationName">
							<li class="active">
								<xsl:value-of select="." />
							</li>
						</xsl:when>
						<xsl:otherwise>
							<li>
								<a>
									<xsl:attribute name="href" select="concat($srcPrefix,'rest/showConfiguration?configuration=',encode-for-uri(.))" />
									<xsl:attribute name="alt" select="." />
									<xsl:attribute name="text" select="." />
									<xsl:value-of select="." />
								</a>
							</li>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:for-each>
			</ul>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>
