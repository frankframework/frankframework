<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<xsl:param name="srcPrefix" />
	<xsl:template match="/">
		<page title="IBIS startup failed">
			<p>
				<b>For some reason, the IBIS application failed to start up.</b>
			</p>
			<p>Please examine the startup-log and try restarting the server.</p>
			<p style="color: red">
				<b>
					<i>NOTE: The IBIS application will automatically retry to startup every minute.</i>
				</b>
			</p>
		</page>
	</xsl:template>
</xsl:stylesheet>
