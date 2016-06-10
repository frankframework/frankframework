<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<xsl:param name="srcPrefix" />
	<xsl:param name="protocolList" />
	<xsl:param name="applicationId" />
	<xsl:param name="serviceId" />
	<xsl:param name="messageProtocol" />
	<xsl:param name="message" />
	<xsl:param name="fileEncoding" />
	<xsl:param name="result" />
	<xsl:variable name="brvbar" select="'&#166;'" />
	<xsl:template match="/">
		<page title="Call an IFSA Service">
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

						//]]&gt;
					</xsl:text>
			</script>
			<form method="post" action="" enctype="multipart/form-data">
				<table border="0" width="100%">
					<tr>
						<td>Application ID</td>
						<td>
							<input class="text" maxlength="180" size="80" type="text"
								name="applicationId">
								<xsl:attribute name="value" select="$applicationId" />
							</input>
						</td>
					</tr>
					<tr>
						<td>Service ID</td>
						<td>
							<input class="text" maxlength="180" size="80" type="text"
								name="serviceId">
								<xsl:attribute name="value" select="$serviceId" />
							</input>
						</td>
					</tr>
					<tr>
						<td>Message Protocol</td>
						<td>
							<xsl:variable name="pl">
								<xsl:choose>
									<xsl:when test="string-length($protocolList)=0">
										<xsl:for-each select="protocols/protocol">
											<xsl:if test="position()&gt;1">
												<xsl:value-of select="$brvbar" />
											</xsl:if>
											<xsl:value-of select="." />
										</xsl:for-each>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="$protocolList" />
									</xsl:otherwise>
								</xsl:choose>
							</xsl:variable>
							<input class="normal" type="hidden" name="protocolList">
								<xsl:attribute name="value" select="$pl" />
							</input>
							<select class="normal" name="messageProtocol">
								<xsl:for-each select="tokenize($pl,$brvbar)">
									<option>
										<xsl:attribute name="value" select="." />
										<xsl:if test="$messageProtocol=.">
											<xsl:attribute name="selected">selected</xsl:attribute>
										</xsl:if>
										<xsl:value-of select="." />
									</option>
								</xsl:for-each>
							</select>
						</td>
					</tr>
					<tr>
						<td>Message</td>
						<td>
							<textarea class="normal" name="message" cols="580" rows="10">
								<xsl:value-of select="$message" />
							</textarea>
						</td>
					</tr>
					<tr>
						<td>Upload File</td>
						<td>
							<input class="file" type="file" name="file" />
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
