<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<xsl:param name="srcPrefix" />
	<xsl:param name="jmsRealmList" />
	<xsl:param name="jmsRealm" />
	<xsl:param name="name" />
	<xsl:param name="version" />
	<xsl:param name="fileEncoding" />
	<xsl:param name="result" />
	<xsl:variable name="brvbar" select="'&#166;'" />
	<xsl:template match="/">
		<page title="Upload Configuration">
			<xsl:if test="name(*)='error'">
				<font color="red">
					<xsl:value-of select="*" />
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

						function fillNameAndVersion(obj) {
							var name = document.getElementById("name").value;
							var version = document.getElementById("version").value;
							var name_new = "";
							var version_new = "";
							var i = obj.value.lastIndexOf(".");
							if (i != -1) {
								name_new = obj.value.substring(0, i);
								var j = name_new.lastIndexOf("-");
								if (j != -1) {
									name_new = name_new.substring(0, j);
									j = name_new.lastIndexOf("-");
									if (j != -1) {
										name_new = obj.value.substring(0, j);
										version_new = obj.value.substring(j + 1, i);
									}
								}
							}
							if (name=="" &amp;&amp; version=="") {
								document.getElementById("name").value=name_new;
								document.getElementById("version").value=version_new;
							} else {
								if (name!=name_new || version!=version_new) {
									var msg = "Overwrite name ["+name+"] with ["+name_new+"] and version ["+version+"] with ["+version_new+"]?";
									if (confirm(msg)) {
										document.getElementById("name").value=name_new;
										document.getElementById("version").value=version_new;
									}
								}
							}
						}

						//]]&gt;
					</xsl:text>
			</script>
			<form method="post" action="" enctype="multipart/form-data">
				<table border="0" width="100%">
					<tr>
						<td>Select a jms realm</td>
						<td>
							<xsl:variable name="jr">
								<xsl:choose>
									<xsl:when test="string-length($jmsRealmList)=0">
										<xsl:for-each select="jmsRealms/jmsRealm">
											<xsl:if test="position()&gt;1">
												<xsl:value-of select="$brvbar" />
											</xsl:if>
											<xsl:value-of select="." />
										</xsl:for-each>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="$jmsRealmList" />
									</xsl:otherwise>
								</xsl:choose>
							</xsl:variable>
							<input class="normal" type="hidden" name="jmsRealmList">
								<xsl:attribute name="value" select="$jr" />
							</input>
							<select class="normal" name="jmsRealm">
								<xsl:for-each select="tokenize($jr,$brvbar)">
									<option>
										<xsl:attribute name="value" select="." />
										<xsl:if test="$jmsRealm=.">
											<xsl:attribute name="selected">selected</xsl:attribute>
										</xsl:if>
										<xsl:value-of select="." />
									</option>
								</xsl:for-each>
							</select>
						</td>
					</tr>
					<tr>
						<td>Name</td>
						<td>
							<input class="text" maxlength="100" size="80" type="text"
								name="name" id="name">
								<xsl:attribute name="value" select="$name" />
							</input>
						</td>
					</tr>
					<tr>
						<td>Version</td>
						<td>
							<input class="text" maxlength="50" size="40" type="text"
								name="version" id="version">
								<xsl:attribute name="value" select="$version" />
							</input>
						</td>
					</tr>
					<tr>
						<td>Upload File</td>
						<td>
							<input class="file" type="file" name="file"
								onchange="fillNameAndVersion(this)" />
							<text>Encoding</text>
							<input class="text" maxlength="20" name="fileEncoding"
								size="10" type="text">
								<xsl:attribute name="value" select="$fileEncoding" />
							</input>
						</td>
					</tr>
					<tr>
						<td>Result</td>
						<td>
							<textarea class="normal" name="result" cols="580" rows="10">
								<xsl:value-of select="$result" />
							</textarea>
						</td>
					</tr>
					<tr>
						<td />
						<td>
							<input type="submit" onmouseover="changeBg(this,true);"
								onmouseout="changeBg(this,false);" value="send" />
						</td>
					</tr>
				</table>
			</form>
		</page>
	</xsl:template>
</xsl:stylesheet>
