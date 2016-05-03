<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<xsl:param name="srcPrefix" />
	<xsl:param name="adapterList" />
	<xsl:param name="adapterName" />
	<xsl:param name="message" />
	<xsl:param name="fileEncoding" />
	<xsl:param name="result" />
	<xsl:param name="state" />
	<xsl:variable name="brvbar" select="'&#166;'" />
	<xsl:template match="/">
		<page title="Test a PipeLine">
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
						<td>Select an adapter</td>
						<td>
							<xsl:variable name="al">
								<xsl:choose>
									<xsl:when test="string-length($adapterList)=0">
										<xsl:for-each select="adapters/adapter">
											<xsl:if test="position()&gt;1">
												<xsl:value-of select="$brvbar" />
											</xsl:if>
											<xsl:value-of select="." />
										</xsl:for-each>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="$adapterList" />
									</xsl:otherwise>
								</xsl:choose>
							</xsl:variable>
							<input class="normal" type="hidden" name="adapterList">
								<xsl:attribute name="value" select="$al" />
							</input>
							<select class="normal" name="adapterName">
								<xsl:for-each select="tokenize($al,$brvbar)">
									<option>
										<xsl:attribute name="value" select="." />
										<xsl:if test="$adapterName=.">
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
						<td>State</td>
						<td>
							<input class="text" maxlength="180" name="state" size="80"
								type="text">
								<xsl:attribute name="value" select="$state" />
							</input>
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
