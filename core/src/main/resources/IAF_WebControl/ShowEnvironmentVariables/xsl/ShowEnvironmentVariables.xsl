<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<xsl:param name="srcPrefix" />
	<xsl:param name="configurationName" />
	<xsl:param name="classLoaderType" />
	<xsl:variable name="brvbar" select="'&#166;'" />
	<xsl:template match="/">
		<page title="Show configuration">
			<xsl:attribute name="title"><xsl:value-of
				select="concat('Show Environment variables: ',$configurationName)" /><xsl:if
				test="string-length($classLoaderType)&gt;0"><xsl:value-of select="concat(' (',$classLoaderType,')')" /></xsl:if></xsl:attribute>
			<xsl:if test="name(*)='error'">
				<font color="red">
					<xsl:value-of select="*" />
				</font>
			</xsl:if>
			<xsl:if test="string-length(root/@message)&gt;0">
				<font color="green">
					<xsl:value-of select="root/@message" />
				</font>
			</xsl:if>
			<script type="text/javascript">
				<xsl:text disable-output-escaping="yes">
						//&lt;![CDATA[

						function changeBg(obj,isOver) {
							var color1="#8D0022";
							var color2="#b4e2ff";
							if (isOver) {
							    obj.style.backgroundColor=color1;
							    obj.style.color=color2;
							} else {
							    obj.style.backgroundColor=color2;
							    obj.style.color=color1;
							}
						}

						//]]&gt;
				</xsl:text>
			</script>
			<form method="post" action="" enctype="multipart/form-data">
				<table>
					<caption class="caption">Dynamic parameters</caption>
					<tr>
						<td class="filterRow">Root Log level</td>
						<td class="filterRow">
							<xsl:variable name="lll"
								select="concat('DEBUG',$brvbar,'INFO',$brvbar,'WARN',$brvbar,'ERROR')" />
							<xsl:variable name="logLevel"
								select="root/dynamicParameters/@logLevel" />
							<select class="normal" name="logLevel">
								<xsl:for-each select="tokenize($lll,$brvbar)">
									<option>
										<xsl:attribute name="value" select="." />
										<xsl:if test="$logLevel=.">
											<xsl:attribute name="selected">selected</xsl:attribute>
										</xsl:if>
										<xsl:value-of select="." />
									</option>
								</xsl:for-each>
							</select>
						</td>
					</tr>
					<tr>
						<td class="filterRow">Log intermediary results</td>
						<td class="filterRow">
							<input class="checkbox" type="checkbox" name="logIntermediaryResults"
								value="on">
								<xsl:if test="root/dynamicParameters/@logIntermediaryResults='true'">
									<xsl:attribute name="checked">checked</xsl:attribute>
								</xsl:if>
							</input>
						</td>
					</tr>
					<tr>
						<td class="filterRow">Length log records</td>
						<td class="filterRow">
							<input class="text" type="text" name="lengthLogRecords"
								maxlength="26" size="8">
								<xsl:attribute name="value"
									select="root/dynamicParameters/@lengthLogRecords" />
							</input>
						</td>
					</tr>
					<tr>
						<td class="filterRow">
							<input type="reset" onmouseover="changeBg(this,true);"
								onmouseout="changeBg(this,false);" value="reset" />
						</td>
						<td class="filterRow">
							<input type="submit" onmouseover="changeBg(this,true);"
								onmouseout="changeBg(this,false);" value="send" />
						</td>
					</tr>
				</table>
			</form>
			<br />
			<br />
			<xsl:call-template name="tabs" />
			<div class="tabpanel">
				<xsl:for-each select="root/environmentVariables/propertySet">
					<xsl:variable name="pos1" select="position()" />
					<xsl:if test="$pos1&gt;1">
						<br />
					</xsl:if>
					<table>
						<caption class="caption">
							<xsl:value-of select="@name" />
						</caption>
						<tr>
							<td class="subHeader">Id</td>
							<td class="subHeader">Property</td>
							<td class="subHeader">Value</td>
						</tr>
						<xsl:for-each select="property">
							<xsl:sort select="@name" />
							<xsl:variable name="pos2" select="position()" />
							<tr>
								<td class="filterRow">
									<xsl:value-of select="concat($pos1,'-',$pos2)" />
								</td>
								<td class="filterRow">
									<xsl:value-of select="@name" />
								</td>
								<td class="filterRow">
									<xsl:value-of select="." />
								</td>
							</tr>
						</xsl:for-each>
					</table>
				</xsl:for-each>
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
									<xsl:attribute name="href"
										select="concat($srcPrefix,'rest/showEnvironmentVariables?configuration=',encode-for-uri(.))" />
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
