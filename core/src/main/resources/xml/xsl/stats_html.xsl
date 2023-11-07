<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>
	<xsl:decimal-format name="nl" decimal-separator="," grouping-separator="."/>
	<xsl:param name="timestamp"/>
	<xsl:param name="adapterName"/>
	<xsl:param name="servletPath"/>
	<xsl:template match="/">
		<html>
			<xsl:call-template name="htmlheading"/>
			<body>
				<table class="page" width="100%">
					<tr>
						<td colspan="3">
							<table width="100%">
								<tr>
									<td>
										<h1>Show Adapter Statistics</h1>
									</td>
									<td/>
								</tr>
							</table>
						</td>
					</tr>
					<tr>
						<td colspan="3"/>
					</tr>
					<xsl:apply-templates select="*"/>
				</table>
			</body>
		</html>
	</xsl:template>
	<xsl:template name="htmlheading">
		<head>
			<link rel="stylesheet" type="text/css" href="iaf/ie4.css"/>
			<title>Show Adapter Statistics</title>
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

						function createAndPostUrl() {
							var paramsArray = new Array();
							/* split url and set url params in array */
							var urlSplit = String(document.location).split('?');
							var docUrl = urlSplit[0];
							var valuePairs = urlSplit[1].split('&amp;');
							for (i=0; i&lt;=(valuePairs.length-1); i++) {
								var urlVarPair = valuePairs[i].split('=');
								paramsArray[urlVarPair[0]] = urlVarPair[1];
							}
							
							/* add or replace params from page */
							var timestampValue = document.getElementById('timestamp').value;
							paramsArray['timestamp'] = timestampValue;
							var adapterNameValue = document.getElementById('adapterName').value;
							paramsArray['adapterName'] = adapterNameValue;
							
							/* concat url */
							docUrl += '?';
							for (var paramValue in paramsArray) {
								docUrl += paramValue+'='+paramsArray[paramValue]+'&amp;';
							}
							document.location = docUrl.substr(0,docUrl.length-1);
						}

						//]]&gt;
						</xsl:text>
			</script>
		</head>
	</xsl:template>
	<xsl:template name="bodyheading">
		<xsl:param name="timestamps"/>
		<xsl:param name="adapters"/>
		<xsl:param name="targetTimestamp" select="$timestamp"/>
		<xsl:param name="targetAdapter" select="$adapterName"/>
		<tr>
			<td width="50px"/>
			<td class="pagePanel">
				<table>
					<tr>
						<td>Select a timestamp</td>
						<td>
							<select class="normal" name="timestamp" id="timestamp">
								<option>-- select a timestamp --</option>
								<xsl:for-each select="$timestamps">
									<option>
										<xsl:attribute name="value"><xsl:value-of select="."/></xsl:attribute>
										<xsl:if test=".=$targetTimestamp">
											<xsl:attribute name="selected"/>
										</xsl:if>
										<xsl:value-of select="."/>
									</option>
								</xsl:for-each>
							</select>
						</td>
					</tr>
					<tr>
						<td>Select an adapter</td>
						<td>
							<select class="normal" name="adapterName" id="adapterName">
								<option>-- select an adapter --</option>
								<xsl:for-each select="$adapters">
									<xsl:sort select="."/>
									<xsl:variable name="name" select="@name"/>
									<xsl:if test="(.=preceding)=false()">
										<option>
											<xsl:attribute name="value"><xsl:value-of select="."/></xsl:attribute>
											<xsl:if test=".=$targetAdapter">
												<xsl:attribute name="selected"/>
											</xsl:if>
											<xsl:value-of select="."/>
										</option>
									</xsl:if>
								</xsl:for-each>
							</select>
						</td>
					</tr>
					<tr>
						<td/>
						<td>
							<input class="submit" onmouseover="changeBg(this,true);" onmouseout="changeBg(this,false);" type="button" value="send" onclick="createAndPostUrl()"/>
						</td>
					</tr>
				</table>
			</td>
		</tr>
		<tr>
			<td colspan="3"/>
		</tr>
	</xsl:template>
	<xsl:key name="statgroups-by-adapter" match="/statisticsCollections/statisticsCollection/statgroup/statgroup" use="@name"/>
	<xsl:template match="statisticsCollections">
		<xsl:variable name="adapters" select="statisticsCollection/statgroup/statgroup[count(. | key('statgroups-by-adapter',@name)[1])=1]/@name"/>
		<xsl:call-template name="bodyheading">
			<xsl:with-param name="timestamps" select="statisticsCollection/@timestamp"/>
			<xsl:with-param name="adapters" select="$adapters"/>
		</xsl:call-template>
		<tr>
			<td width="50px"/>
			<td class="pagePanel">
				<xsl:apply-templates select="statisticsCollection[@timestamp=$timestamp]/statgroup/statgroup[@name=$adapterName]"/>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="statgroup">
		<table>
			<caption class="caption">Adapter Statistics</caption>
			<tr>
				<th class="colHeader">Name</th>
				<th class="colHeader">up since</th>
				<th class="colHeader">Last Message</th>
				<th class="colHeader">Messages processed</th>
				<th class="colHeader">Messages in error</th>
				<th class="colHeader">Messages in process</th>
			</tr>
			<tr>
				<td class="filterRow">
					<xsl:value-of select="@name"/>
				</td>
				<td class="filterRow">
					<xsl:value-of select="item[@name='upSince']/@value"/>
				</td>
				<td class="filterRow">
					<xsl:value-of select="item[@name='lastMessageDate']/@value"/>
				</td>
				<td align="right" class="filterRow">
					<xsl:value-of select="item[@name='messagesProcessed']/@value"/>
				</td>
				<td align="right" class="filterRow">
					<xsl:value-of select="item[@name='messagesInError']/@value"/>
				</td>
				<td align="right" class="filterRow">
					<xsl:value-of select="item[@name='messagesInProcess']/@value"/>
				</td>
			</tr>
		</table>
		<br/>
		<br/>
		<br/>
		<table>
			<caption class="caption">Total message processing duration (in ms)</caption>
			<xsl:for-each select="stat/cumulative/item">
				<tr>
					<xsl:attribute name="class"><xsl:choose><xsl:when test="(position() mod 2) = 0">rowEven</xsl:when><xsl:otherwise>filterRow</xsl:otherwise></xsl:choose></xsl:attribute>
					<td colspan="2" class="filterRow">
						<xsl:value-of select="@name"/>
					</td>
					<td align="right" colspan="2" class="filterRow">
						<xsl:call-template name="formatValue">
							<xsl:with-param name="value" select="@value"/>
						</xsl:call-template>
					</td>
				</tr>
			</xsl:for-each>
		</table>
		<br/>
		<br/>
		<br/>
		<table>
			<caption class="caption">Adapter statistics by the hour</caption>
			<tr>
				<th class="colHeader">start time</th>
				<xsl:for-each select="statgroup[@type='processing by hour']/item">
					<td colspan="2" class="filterRow">
						<xsl:value-of select="@name"/>
					</td>
				</xsl:for-each>
			</tr>
			<tr>
				<th class="colHeader">count</th>
				<xsl:for-each select="statgroup[@type='processing by hour']/item">
					<td align="right" colspan="2" class="filterRow">
						<xsl:value-of select="@value"/>
					</td>
				</xsl:for-each>
			</tr>
		</table>
		<br/>
		<br/>
		<br/>
		<table>
			<caption class="caption">Counts for receivers</caption>
			<tr>
				<th class="colHeader">name</th>
				<th class="colHeader">messages received/retried</th>
			</tr>
			<xsl:for-each select="statgroup[@type='receivers']/statgroup[@type='receiver']">
				<tr>
					<xsl:attribute name="class"><xsl:choose><xsl:when test="(position() mod 2) = 0">rowEven</xsl:when><xsl:otherwise>filterRow</xsl:otherwise></xsl:choose></xsl:attribute>
					<td class="filterRow">
						<xsl:value-of select="@name"/>
					</td>
					<td class="filterRow">
						<xsl:value-of select="concat(item[@name='messagesReceived']/@value,'/',item[@name='messagesRetried']/@value)"/>
					</td>
				</tr>
			</xsl:for-each>
		</table>
		<br/>
		<br/>
		<br/>
		<table>
			<caption class="caption">Process statistics for receivers (in ms)</caption>
			<tr>
				<th class="colHeader">receiver</th>
				<th class="colHeader">threads processing</th>
				<xsl:for-each select="(statgroup[@type='receivers']/statgroup[@type='receiver']/statgroup[@type='procStats']/stat)[1]/cumulative/item">
					<th class="colHeader">
						<xsl:value-of select="@name"/>
					</th>
				</xsl:for-each>
			</tr>
			<xsl:for-each select="statgroup[@type='receivers']/statgroup[@type='receiver']/statgroup[@type='procStats']/stat">
				<tr>
					<xsl:attribute name="class"><xsl:choose><xsl:when test="(position() mod 2) = 0">rowEven</xsl:when><xsl:otherwise>filterRow</xsl:otherwise></xsl:choose></xsl:attribute>
					<td class="filterRow">
						<xsl:value-of select="../../@name"/>
					</td>
					<td class="filterRow">
						<xsl:value-of select="@name"/>
					</td>
					<xsl:for-each select="cumulative/item">
						<td align="right" class="filterRow">
							<xsl:call-template name="formatValue">
								<xsl:with-param name="value" select="@value"/>
							</xsl:call-template>
						</td>
					</xsl:for-each>
				</tr>
			</xsl:for-each>
		</table>
		<br/>
		<br/>
		<br/>
		<table>
			<caption class="caption">Duration statistics per pipe (in ms)</caption>
			<tr>
				<th class="colHeader">name</th>
				<xsl:for-each select="(statgroup[@type='pipeline']/statgroup[@type='duration']/stat)[1]/cumulative/item">
					<th class="colHeader">
						<xsl:value-of select="@name"/>
					</th>
				</xsl:for-each>
			</tr>
			<xsl:for-each select="statgroup[@type='pipeline']/statgroup[@type='duration']/stat">
				<tr>
					<xsl:attribute name="class"><xsl:choose><xsl:when test="(position() mod 2) = 0">rowEven</xsl:when><xsl:otherwise>filterRow</xsl:otherwise></xsl:choose></xsl:attribute>
					<td class="filterRow">
						<xsl:value-of select="@name"/>
					</td>
					<xsl:for-each select="cumulative/item">
						<td align="right" class="filterRow">
							<xsl:call-template name="formatValue">
								<xsl:with-param name="value" select="@value"/>
							</xsl:call-template>
						</td>
					</xsl:for-each>
				</tr>
			</xsl:for-each>
		</table>
		<br/>
		<br/>
		<br/>
		<table>
			<caption class="caption">Size statistics per pipe (in bytes)</caption>
			<tr>
				<th class="colHeader">name</th>
				<xsl:for-each select="(statgroup[@type='pipeline']/statgroup[@type='size']/stat)[1]/cumulative/item">
					<th class="colHeader">
						<xsl:value-of select="@name"/>
					</th>
				</xsl:for-each>
			</tr>
			<xsl:for-each select="statgroup[@type='pipeline']/statgroup[@type='size']/stat">
				<tr>
					<xsl:attribute name="class"><xsl:choose><xsl:when test="(position() mod 2) = 0">rowEven</xsl:when><xsl:otherwise>filterRow</xsl:otherwise></xsl:choose></xsl:attribute>
					<td class="filterRow">
						<xsl:value-of select="@name"/>
					</td>
					<xsl:for-each select="cumulative/item">
						<td align="right" class="filterRow">
							<xsl:call-template name="formatValue">
								<xsl:with-param name="value" select="@value"/>
							</xsl:call-template>
						</td>
					</xsl:for-each>
				</tr>
			</xsl:for-each>
		</table>
	</xsl:template>
	<xsl:key name="item-by-name" match="/overview/data/stat/summary/item" use="@name"/>
	<xsl:template match="overview">
		<xsl:variable name="targetTimestamp" select="timestamps/@targetTimestamp"/>
		<xsl:variable name="targetAdapter" select="adapters/@targetAdapter"/>
		<!-- 		<xsl:variable name="pipeSplit"   select="adapters/@pipeSplit" /> -->
		<xsl:variable name="firstCol">
			<xsl:choose>
				<xsl:when test="$targetAdapter">timestamp</xsl:when>
				<xsl:otherwise>adapter</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:call-template name="bodyheading">
			<xsl:with-param name="timestamps" select="timestamps/timestamp/@value"/>
			<xsl:with-param name="adapters" select="adapters/adapter/@name"/>
			<xsl:with-param name="targetTimestamp" select="$targetTimestamp"/>
			<xsl:with-param name="targetAdapter" select="$targetAdapter"/>
		</xsl:call-template>
		<xsl:variable name="itemnames" select="data/stat/summary/item[count(. | key('item-by-name',@name)[1])=1]/@name"/>
		<tr>
			<td width="50px"/>
			<td class="pagePanel">
				<table>
					<caption class="caption">Adapter Statistics</caption>
					<th class="colHeader">
						<xsl:value-of select="$firstCol"/>
					</th>
					<xsl:for-each select="$itemnames">
						<th class="colHeader">
							<xsl:value-of select="."/>
						</th>
					</xsl:for-each>
					<xsl:for-each select="data/stat">
						<xsl:variable name="summary" select="summary"/>
						<tr>
							<td class="filterRow">
								<xsl:variable name="href">
									<xsl:value-of select="$servletPath"/>
									<xsl:choose>
										<xsl:when test="$firstCol!='adapter'">adapterName=<xsl:value-of select="$targetAdapter"/>&amp;timestamp=<xsl:value-of select="@name"/>
										</xsl:when>
										<xsl:otherwise>adapterName=<xsl:value-of select="@name"/>&amp;timestamp=<xsl:value-of select="$targetTimestamp"/>
										</xsl:otherwise>
									</xsl:choose>
								</xsl:variable>
								<!-- 
								(<xsl:element name="a">
									<xsl:attribute name="href" ><xsl:value-of select="$href"/>&amp;pipeSplit=true</xsl:attribute>
									+
								</xsl:element>)
								 -->
								<xsl:element name="a">
									<xsl:attribute name="href"><xsl:value-of select="$href"/></xsl:attribute>
									<xsl:value-of select="@name"/>
								</xsl:element>
							</td>
							<xsl:for-each select="$itemnames">
								<xsl:variable name="itemname" select="."/>
								<td align="right" class="filterRow">
									<xsl:call-template name="formatValue">
										<xsl:with-param name="value" select="$summary/item[@name=$itemname]/@value"/>
									</xsl:call-template>
								</td>
							</xsl:for-each>
						</tr>
					</xsl:for-each>
				</table>
			</td>
		</tr>
	</xsl:template>
	<xsl:template name="formatValue">
		<xsl:param name="value"/>
		<xsl:variable name="val" select="translate($value,',','.')"/>
		<xsl:choose>
			<xsl:when test="string(number($val))='NaN'">
				<xsl:value-of select="$value"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="format-number(number($val),'#.##0','nl')"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
