<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<xsl:param name="srcPrefix" />
	<xsl:param name="jmsRealmList" />
	<xsl:param name="jmsRealm" />
	<xsl:param name="actionList" />
	<xsl:param name="action" />
	<xsl:param name="name" />
	<xsl:param name="value" />
	<xsl:param name="result" />
	<xsl:variable name="brvbar" select="'&#166;'" />
	<xsl:template match="/">
		<page title="Execute Jdbc Properties">
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
						<td>Action</td>
						<td>
							<xsl:variable name="at">
								<xsl:choose>
									<xsl:when test="string-length($actionList)=0">
										<xsl:text>insert</xsl:text>
										<xsl:value-of select="$brvbar" />
										<xsl:text>update</xsl:text>
										<xsl:value-of select="$brvbar" />
										<xsl:text>delete</xsl:text>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="$actionList" />
									</xsl:otherwise>
								</xsl:choose>
							</xsl:variable>
							<input class="normal" type="hidden" name="actionList">
								<xsl:attribute name="value" select="$at" />
							</input>
							<select class="normal" name="action">
								<xsl:for-each select="tokenize($at,$brvbar)">
									<option>
										<xsl:attribute name="value" select="." />
										<xsl:if test="$action=.">
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
						<td>Value</td>
						<td>
							<input class="text" maxlength="100" size="80" type="text"
								name="value" id="value">
								<xsl:attribute name="value" select="$value" />
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
