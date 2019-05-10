<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<xsl:param name="srcPrefix" />
	<xsl:param name="defaultDestination" />
	<xsl:template match="/">
		<page title="Upload Files">
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
	
					function switchVars(action) {
						if (action == 'open') {
							document.getElementById("variables").style.display = "block";
							document.getElementById("open").style.display = "none";
							document.getElementById("close").style.display = "inline-block";
						} else {
							document.getElementById("variables").style.display = "none";
							document.getElementById("open").style.display = "inline-block";
							document.getElementById("close").style.display = "none";
						}
					}
	
					//]]&gt;
					</xsl:text>
			</script>
			<form method="post" action="" enctype="multipart/form-data">
				<table border="0" width="100%">
					<tr>
						<td>Upload zip file</td>
						<td>
							<input class="file" type="file" name="file" value="" />
						</td>
					</tr>
					<tr>
						<td>Destination directory</td>
						<td>
							<input class="text" type="text" name="destination"
								maxlength="180" size="60">
								<xsl:attribute name="value" select="$defaultDestination" />
							</input>
						</td>
					</tr>
					<tr>
						<td>Clean destination directory</td>
						<td>
							<input class="checkbox" type="checkbox" name="cleanFilesystem"
								value="on" />
						</td>
					</tr>
					<tr>
						<td />
						<td>
							<input class="submit" onmouseover="changeBg(this,true);"
								onmouseout="changeBg(this,false);" type="submit" value="send" />
						</td>
					</tr>
				</table>
			</form>
		</page>
	</xsl:template>
</xsl:stylesheet>
