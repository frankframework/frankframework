<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>
	<xsl:param name="timestamp"/>
	<xsl:param name="adapterName"/>

	<xsl:template match="/">
		<html>
			<xsl:call-template name="htmlheading" />
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

	<xsl:template name="htmlheading" >
			<head>
				<link rel="stylesheet" type="text/css" href="ie4.css"/>
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

	<xsl:template name="bodyheading" >
		<xsl:param name="timestamps" />
		<xsl:param name="adapters" />
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
										<xsl:if test=".=$timestamp">
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
											<xsl:if test=".=$adapterName">
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

	<xsl:key name="statgroups-by-adapter" match="/statisticsCollections/statisticsCollection/statgroup/statgroup" use="@name" />
	
	<xsl:template match="statisticsCollections">
		<xsl:variable name="adapters" select="statisticsCollection/statgroup/statgroup[count(. | key('statgroups-by-adapter',@name)[1])=1]/@name" />
		<xsl:call-template name="bodyheading">
			<xsl:with-param name="timestamps" select="statisticsCollection/@timestamp" />
			<xsl:with-param name="adapters" select="$adapters" />
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
					<xsl:value-of select="item[@name='name']/@value"/>
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
			<caption class="caption">Total message processing duration<img src="images/pixel.gif" width="300px" height="1px"/>
			</caption>
			<xsl:for-each select="stat/cumulative/item">
				<tr>
					<xsl:attribute name="class"><xsl:choose><xsl:when test="(position() mod 2) = 0">rowEven</xsl:when><xsl:otherwise>filterRow</xsl:otherwise></xsl:choose></xsl:attribute>
					<td colspan="2" class="filterRow">
						<xsl:value-of select="@name"/>
					</td>
					<td align="right" colspan="2" class="filterRow">
						<xsl:value-of select="@value"/>
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
			<caption class="caption">counts for receivers</caption>
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
		<xsl:for-each select="statgroup[@type='receivers']/statgroup[@type='receiver']">
			<table>
				<caption class="caption">process statistics for receivers</caption>
				<tr>
					<th class="colHeader">receiver</th>
					<th class="colHeader">threads processing</th>
					<th class="colHeader">count</th>
					<th class="colHeader">min</th>
					<th class="colHeader">max</th>
					<th class="colHeader">avg</th>
					<th class="colHeader">stdDev</th>
					<th class="colHeader">first</th>
					<th class="colHeader">last</th>
					<th class="colHeader">sum</th>
					<th class="colHeader">&lt; 100ms</th>
					<th class="colHeader">&lt; 1000ms</th>
					<th class="colHeader">&lt; 2000ms</th>
					<th class="colHeader">&lt; 10000ms</th>
					<th class="colHeader">p50</th>
					<th class="colHeader">p90</th>
					<th class="colHeader">p95</th>
					<th class="colHeader">p98</th>
				</tr>
				<xsl:for-each select="statgroup[@type='procStats']/stat">
					<tr>
						<xsl:attribute name="class"><xsl:choose><xsl:when test="(position() mod 2) = 0">rowEven</xsl:when><xsl:otherwise>filterRow</xsl:otherwise></xsl:choose></xsl:attribute>
						<td class="filterRow">
							<xsl:value-of select="../../@name"/>
						</td>
						<td class="filterRow">
							<xsl:value-of select="@name"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='count']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='min']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='max']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='avg']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='stdDev']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='first']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='last']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='sum']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='&lt; 100ms']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='&lt; 1000ms']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='&lt; 2000ms']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='&lt; 10000ms']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='p50']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='p90']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='p95']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='p98']/@value"/>
						</td>
					</tr>
				</xsl:for-each>
			</table>
		</xsl:for-each>
		<br/>
		<br/>
		<br/>
		<xsl:for-each select="statgroup[@type='pipeline']">
			<table>
				<caption class="caption">Duration statistics per Pipe</caption>
				<tr>
					<th class="colHeader">name</th>
					<th class="colHeader">count</th>
					<th class="colHeader">min</th>
					<th class="colHeader">max</th>
					<th class="colHeader">avg</th>
					<th class="colHeader">stdDev</th>
					<th class="colHeader">first</th>
					<th class="colHeader">last</th>
					<th class="colHeader">sum</th>
					<th class="colHeader">&lt; 100ms</th>
					<th class="colHeader">&lt; 1000ms</th>
					<th class="colHeader">&lt; 2000ms</th>
					<th class="colHeader">&lt; 10000ms</th>
					<th class="colHeader">p50</th>
					<th class="colHeader">p90</th>
					<th class="colHeader">p95</th>
					<th class="colHeader">p98</th>
				</tr>
				<xsl:for-each select="statgroup[@type='pipeStats']/stat">
					<tr>
						<xsl:attribute name="class"><xsl:choose><xsl:when test="(position() mod 2) = 0">rowEven</xsl:when><xsl:otherwise>filterRow</xsl:otherwise></xsl:choose></xsl:attribute>
						<td class="filterRow">
							<xsl:value-of select="@name"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='count']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='min']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='max']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='avg']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='stdDev']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='first']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='last']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='sum']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='&lt; 100ms']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='&lt; 1000ms']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='&lt; 2000ms']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='&lt; 10000ms']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='p50']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='p90']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='p95']/@value"/>
						</td>
						<td align="right" class="filterRow">
							<xsl:value-of select="cumulative/item[@name='p98']/@value"/>
						</td>
					</tr>
				</xsl:for-each>
			</table>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:key name="item-by-name" match="/overview/data/stat/summary/item" use="@name" />

	<xsl:template match="overview">
		<xsl:call-template name="bodyheading">
			<xsl:with-param name="timestamps" select="timestamps/timestamp/@value" />
			<xsl:with-param name="adapters" select="data/stat/@name" />
		</xsl:call-template>
		<xsl:variable name="itemnames" select="data/stat/summary/item[count(. | key('item-by-name',@name)[1])=1]/@name" />
		<tr>
			<td width="50px"/>
			<td class="pagePanel">
				<table>
					<caption class="caption">Adapter Statistics</caption>
					<th class="colHeader">adapter</th>
					<xsl:for-each select="$itemnames">
						<th class="colHeader"><xsl:value-of select="."/></th>
					</xsl:for-each>
					<xsl:for-each select="data/stat">
						<xsl:variable name="summary" select="summary" />
						<tr>
							<td class="filterRow"><xsl:value-of select="@name"/></td>
							<xsl:for-each select="$itemnames">
								<xsl:variable name="itemname" select="."/>
								<td align="right" class="filterRow">
									<xsl:value-of select="$summary/item[@name=$itemname]/@value"/>
								</td>
							</xsl:for-each>
						</tr>
					</xsl:for-each>
				</table>
			</td>
		</tr>
	</xsl:template>

</xsl:stylesheet>
