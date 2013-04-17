<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ibisDateUtils="xalan://nl.nn.adapterframework.util.DateUtils" xmlns:ibisMisc="xalan://nl.nn.adapterframework.util.Misc" xmlns:ibisXmlUtils="xalan://nl.nn.adapterframework.util.XmlUtils" xmlns:log4j="http://jakarta.apache.org/log4" version="1.0">
	<xsl:output method="xml" omit-xml-declaration="yes"/>
	<xsl:template match="log4j:log4j">
		<html>
			<head>
				<title>log4j</title>
				<style type="text/css">
					a
					{
					text-decoration: none;
					cursor: auto;
					color: black;
					}
					td
					{
					font-family: courier;
					font-size: 10pt; 
					}
					th
					{
					font-family: courier;
					font-size: 10pt; 
					font-color: black;
					}
				</style>
				<script type="text/javascript">
					<xsl:text disable-output-escaping="yes">
						//&lt;![CDATA[

						var focusId = 0;

						if (document.all) {  // IE
							document.onkeydown = getKey;
						} else {  // Mozilla
							document.onkeypress = getKey;
						}

						function getInnerText(el) {
							if (document.all) {  // IE
								return el.innerText;
							} else {  // Mozilla
								var str = "";
								for (var i=0; i&lt;el.childNodes.length; i++) {
									switch (el.childNodes.item(i).nodeType) {
										case 1: //ELEMENT_NODE
											str += getInnerText(el.childNodes.item(i));
											break;
										case 3: //TEXT_NODE
											str += el.childNodes.item(i).nodeValue;
											break;
									}
								}
								return str;
							}
						}

						function rowClick(el,row) {
							initEvents(0);
							hiLight(el,row);
						}

						function hiLight(el,rowId) {
							focusId = rowId;
							el.style.backgroundColor="yellow";
							document.getElementById("row"+rowId).focus();
						}

						function rowFocus(rowId) {
							detailsId.innerHTML=rowId;
							detailsTimestamp.innerHTML=getInnerText(document.getElementById("timestamp"+rowId));
							detailsLevel.innerHTML=getInnerText(document.getElementById("level"+rowId));
							detailsNDC.innerHTML=getInnerText(document.getElementById("NDC"+rowId));
							detailsThread.innerHTML=getInnerText(document.getElementById("thread"+rowId));
							detailsLogger.innerHTML=getInnerText(document.getElementById("logger"+rowId));
							detailsMessage.innerHTML=getInnerText(document.getElementById("message"+rowId));
							if (getInnerText(document.getElementById("throwable"+rowId))!="") {
								detailsThrowableText.innerHTML="&lt;b&gt;throwable:&lt;/b&gt;";
							} else {
								detailsThrowableText.innerHTML="";
							}
							detailsThrowable.innerHTML=getInnerText(document.getElementById("throwable"+rowId));
						}

						function filter() {
							var rows=events.getElementsByTagName("tr");
							var filterLevel = document.getElementById("filterLevel").value;
							var filterNDC = document.getElementById("filterNDC").value;
							var filterMessage = document.getElementById("filterMessage").value;
							for(var i=0;i&lt;rows.length;i++) {
								var display="";
								if (filterLevel!="0") {
									var levelNr = getInnerText(document.getElementById("levelNr"+(i+1)));
									if (levelNr&lt;filterLevel) {
										display="none";
									} 
								}
								if (display=="" &amp;&amp; filterNDC!="") {
									var NDC = getInnerText(document.getElementById("NDC"+(i+1)));
									if (NDC.indexOf(filterNDC)==-1) {
										display="none";
									} 
								}
								if (display=="" &amp;&amp; filterMessage!="") {
									var message = getInnerText(document.getElementById("message"+(i+1)));
									if (message.indexOf(filterMessage)==-1) {
										display="none";
									} 
								}
								rows[i].style.display=display;
							}
							initFilterSubmit();
							initDetails();
							initEvents(-99999);
						}

						function clearFilter() {
							filterLevel.value="0";
							filterNDC.value="";
							filterMessage.value="";
							filter();
						}

						function start() {
							initFilterSubmit();
							initEvents(-99999);
						}

						function changeBg(obj,isOver) {
							if (isOver) {
							    obj.style.backgroundColor="ButtonHighLight";
							} else {
							    obj.style.backgroundColor="ButtonFace";
							}
						}

						function getKey(e) {
							if (!e) {
								e = event;
							}
							var key = e.which || e.keyCode;
							switch(key) {
								case 33: //pageUp
									initEvents(-20);
									return false;
								case 34: //pageDown
									initEvents(+20);
									return false;
								case 35: //end
									initEvents(+99999);
									return false;
								case 36: //home
									initEvents(-99999);
									return false;
								case 38: //arrowUp
									initEvents(-1);
									return false;
								case 40: //arrowDown
									initEvents(+1);
									return false;
							}
						}

						function initEvents(ar) {
							var rows=events.getElementsByTagName("tr");
							var maxRowId=0;
							var minRowId=rows.length+1;
							for(var i=0;i&lt;rows.length;i++) {
								rows[i].style.backgroundColor="Window";
								if (rows[i].style.display=="") {
									if (i+1&lt;minRowId) {
										minRowId=i+1;
									}
									if (i+1&gt;maxRowId) {
										maxRowId=i+1;
									}
								}
							}
							if (ar!=0) {
								var j=focusId;

								if (ar&gt;0) {
									while (ar&gt;0) {
										j++;
										if (j&gt;=maxRowId) {
											j=maxRowId;
											ar=0;
										} else {
											if (rows[j-1].style.display=="") {
												ar--;
											}
										}
									}
								} else {
									while (ar&lt;0) {
										j--;
										if (j&lt;=minRowId) {
											j=minRowId;
											ar=0;
										} else {
											if (rows[j-1].style.display=="") {
												ar++;
											}
										}
									}
								}
								if (j&gt;0 &amp;&amp; j&lt;rows.length+1) {
									hiLight(rows[j-1],j);
								}
							}
						}

						function initDetails() {
							detailsId.innerHTML="";
							detailsTimestamp.innerHTML="";
							detailsLevel.innerHTML="";
							detailsNDC.innerHTML="";
							detailsThread.innerHTML="";
							detailsLogger.innerHTML="";
							detailsMessage.innerHTML="";
							detailsThrowableText.innerHTML="";
							detailsThrowable.innerHTML="";
						}

						function initFilterSubmit() {
							switch(1*document.getElementById("filterLevel").value) {
								case 0:
									filterLevelSubmit.innerHTML="[DEBUG]";
									break;
								case 1:
									filterLevelSubmit.innerHTML="[INFO]";
									break;
								case 2:
									filterLevelSubmit.innerHTML="[WARN]";
									break;
								case 3:
									filterLevelSubmit.innerHTML="[ERROR]";
									break;
								case 4:
									filterLevelSubmit.innerHTML="[FATAL]";
									break;
							}
							filterNDCSubmit.innerHTML="["+document.getElementById("filterNDC").value+"]";
							filterMessageSubmit.innerHTML="["+document.getElementById("filterMessage").value+"]";
						}

						//]]&gt;
						</xsl:text>
				</script>
			</head>
			<body onload="start()">
				<div style="overflow:auto; width:1210px; height:14%;">
					<table border="0">
						<tr>
							<td class="questioncell">Filter level:</td>
							<td class="answercell">
								<select name="filterLevel" id="filterLevel" class="text">
									<option value="0">DEBUG</option>
									<option value="1">INFO</option>
									<option value="2">WARN</option>
									<option value="3">ERROR</option>
									<option value="4">FATAL</option>
								</select>
							</td>
							<td id="filterLevelSubmit"/>
						</tr>
						<tr>
							<td class="questioncell">Filter NDC:</td>
							<td class="answercell">
								<input type="Text" name="filterNDC" size="50" maxlength="180" id="filterNDC" class="text"/>
							</td>
							<td id="filterNDCSubmit"/>
						</tr>
						<tr>
							<td class="questioncell">Filter message:</td>
							<td class="answercell">
								<input type="Text" name="filterMessage" size="50" maxlength="180" id="filterMessage" class="text"/>
							</td>
							<td id="filterMessageSubmit"/>
						</tr>
						<tr>
							<td>
								<xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
							</td>
							<td>
								<input class="submit" onmouseover="changeBg(this,true)" onmouseout="changeBg(this,false)" type="button" onClick="filter()" value="filter"/>
								<xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
								<input class="clear" onmouseover="changeBg(this,true)" onmouseout="changeBg(this,false)" type="button" onClick="clearFilter()" value="clear"/>
							</td>
						</tr>
					</table>
				</div>
				<hr/>
				<table width="1210" border="0" cellspacing="0" cellpadding="0" bgcolor="silver">
					<tr>
						<th width="40">id</th>
						<th width="160">timestamp</th>
						<th width="50">level</th>
						<th width="205">NDC</th>
						<th width="205">thread</th>
						<th width="205">logger</th>
						<th width="345">message</th>
					</tr>
				</table>
				<div id="events" style="overflow:auto; width:1210px; height:36%; table-layout:fixed;">
					<table width="1194" border="1" cellspacing="0" cellpadding="0">
						<xsl:apply-templates select="log4j:event"/>
					</table>
				</div>
				<hr/>
				<div id="details" style="overflow:auto; width:1210px; height:44%; table-layout:fixed;">
					<table width="1194" border="0" cellspacing="0" cellpadding="0">
						<tr>
							<td width="10%" valign="top">
								<b>id:</b>
							</td>
							<td id="detailsId"/>
						</tr>
						<tr>
							<td width="10%" valign="top">
								<b>timestamp:</b>
							</td>
							<td id="detailsTimestamp"/>
						</tr>
						<tr>
							<td width="10%" valign="top">
								<b>level:</b>
							</td>
							<td id="detailsLevel"/>
						</tr>
						<tr>
							<td width="10%" valign="top">
								<b>NDC:</b>
							</td>
							<td id="detailsNDC"/>
						</tr>
						<tr>
							<td width="10%" valign="top">
								<b>thread:</b>
							</td>
							<td id="detailsThread"/>
						</tr>
						<tr>
							<td width="10%" valign="top">
								<b>logger:</b>
							</td>
							<td id="detailsLogger"/>
						</tr>
						<tr>
							<td width="10%" valign="top">
								<b>message:</b>
							</td>
							<td id="detailsMessage"/>
						</tr>
						<tr>
							<td id="detailsThrowableText" width="10%" valign="top"/>
							<td id="detailsThrowable"/>
						</tr>
					</table>
				</div>
			</body>
		</html>
	</xsl:template>
	<xsl:template match="log4j:event">
		<xsl:variable name="id">
			<xsl:value-of select="position()"/>
		</xsl:variable>
		<xsl:variable name="timestamp">
			<xsl:value-of select="ibisDateUtils:format(@timestamp,'yyyy-MM-dd-HH:mm:ss')"/>
		</xsl:variable>
		<xsl:variable name="NDC">
			<xsl:value-of select="ibisMisc:replace(ibisXmlUtils:encodeChars(log4j:NDC),'&#xA;','&lt;br&gt;')"/>
		</xsl:variable>
		<xsl:variable name="message">
			<xsl:value-of select="ibisMisc:replace(ibisXmlUtils:encodeChars(log4j:message),'&#xA;','&lt;br&gt;')"/>
		</xsl:variable>
		<xsl:variable name="throwable">
			<xsl:value-of select="ibisMisc:replace(ibisXmlUtils:encodeChars(log4j:throwable),'&#xA;','&lt;br&gt;')"/>
		</xsl:variable>
		<tr>
			<xsl:variable name="onclick">
				<xsl:value-of select="concat('rowClick(this,',$id,')')"/>
			</xsl:variable>
			<xsl:attribute name="onclick"><xsl:value-of select="$onclick"/></xsl:attribute>
			<td>
				<a>
					<xsl:attribute name="href">#</xsl:attribute>
					<xsl:attribute name="id"><xsl:value-of select="concat('row',$id)"/></xsl:attribute>
					<xsl:variable name="onfocus">
						<xsl:value-of select="concat('rowFocus(',$id,')')"/>
					</xsl:variable>
					<xsl:attribute name="onfocus"><xsl:value-of select="$onfocus"/></xsl:attribute>
					<xsl:call-template name="format">
						<xsl:with-param name="string">
							<xsl:value-of select="$id"/>
						</xsl:with-param>
						<xsl:with-param name="length">5</xsl:with-param>
						<xsl:with-param name="alignRight">true</xsl:with-param>
					</xsl:call-template>
				</a>
			</td>
			<td>
				<xsl:call-template name="format">
					<xsl:with-param name="string">
						<xsl:value-of select="$timestamp"/>
					</xsl:with-param>
					<xsl:with-param name="length">20</xsl:with-param>
				</xsl:call-template>
			</td>
			<td>
				<xsl:call-template name="format">
					<xsl:with-param name="string">
						<xsl:value-of select="@level"/>
					</xsl:with-param>
					<xsl:with-param name="length">6</xsl:with-param>
				</xsl:call-template>
			</td>
			<td>
				<xsl:call-template name="format">
					<xsl:with-param name="string">
						<xsl:value-of select="$NDC"/>
					</xsl:with-param>
					<xsl:with-param name="length">25</xsl:with-param>
				</xsl:call-template>
			</td>
			<td>
				<xsl:call-template name="format">
					<xsl:with-param name="string">
						<xsl:value-of select="@thread"/>
					</xsl:with-param>
					<xsl:with-param name="length">25</xsl:with-param>
				</xsl:call-template>
			</td>
			<td>
				<xsl:call-template name="format">
					<xsl:with-param name="string">
						<xsl:value-of select="@logger"/>
					</xsl:with-param>
					<xsl:with-param name="length">25</xsl:with-param>
				</xsl:call-template>
			</td>
			<td>
				<xsl:call-template name="format">
					<xsl:with-param name="string">
						<xsl:value-of select="$message"/>
					</xsl:with-param>
					<xsl:with-param name="length">40</xsl:with-param>
				</xsl:call-template>
			</td>
			<td style="display:none">
				<xsl:attribute name="id"><xsl:value-of select="concat('timestamp',$id)"/></xsl:attribute>
				<xsl:value-of select="$timestamp"/>
			</td>
			<td style="display:none">
				<xsl:attribute name="id"><xsl:value-of select="concat('level',$id)"/></xsl:attribute>
				<xsl:value-of select="@level"/>
			</td>
			<td style="display:none">
				<xsl:attribute name="id"><xsl:value-of select="concat('levelNr',$id)"/></xsl:attribute>
				<xsl:choose>
					<xsl:when test="@level='DEBUG'">0</xsl:when>
					<xsl:when test="@level='INFO'">1</xsl:when>
					<xsl:when test="@level='WARN'">2</xsl:when>
					<xsl:when test="@level='ERROR'">3</xsl:when>
					<xsl:when test="@level='FATAL'">4</xsl:when>
				</xsl:choose>
			</td>
			<td style="display:none">
				<xsl:attribute name="id"><xsl:value-of select="concat('NDC',$id)"/></xsl:attribute>
				<xsl:value-of select="$NDC"/>
			</td>
			<td style="display:none">
				<xsl:attribute name="id"><xsl:value-of select="concat('thread',$id)"/></xsl:attribute>
				<xsl:value-of select="@thread"/>
			</td>
			<td style="display:none">
				<xsl:attribute name="id"><xsl:value-of select="concat('logger',$id)"/></xsl:attribute>
				<xsl:value-of select="@logger"/>
			</td>
			<td style="display:none">
				<xsl:attribute name="id"><xsl:value-of select="concat('message',$id)"/></xsl:attribute>
				<xsl:value-of select="$message"/>
			</td>
			<td style="display:none">
				<xsl:attribute name="id"><xsl:value-of select="concat('throwable',$id)"/></xsl:attribute>
				<xsl:value-of select="$throwable"/>
			</td>
		</tr>
	</xsl:template>
	<xsl:template name="format">
		<xsl:param name="string"/>
		<xsl:param name="length"/>
		<xsl:param name="alignRight"/>
		<xsl:variable name="stringLength">
			<xsl:value-of select="string-length($string)"/>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="$stringLength&gt;$length">
				<xsl:value-of select="concat(substring($string,1,$length - 1),'~')"/>
			</xsl:when>
			<xsl:when test="$stringLength&lt;$length">
				<xsl:choose>
					<xsl:when test="$alignRight='true'">
						<xsl:call-template name="repeat-space">
							<xsl:with-param name="n" select="$length - $stringLength"/>
						</xsl:call-template>
						<xsl:value-of select="$string"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="$string"/>
						<xsl:call-template name="repeat-space">
							<xsl:with-param name="n" select="$length - $stringLength"/>
						</xsl:call-template>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$string"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template name="repeat-space">
		<xsl:param name="n"/>
		<xsl:if test="$n != 0">
			<xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
			<xsl:call-template name="repeat-space">
				<xsl:with-param name="n" select="$n - 1"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>
