<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" doctype-system="about:legacy-compat" />
	<xsl:include href="functions.xsl"/>
	<xsl:include href="components.xsl"/>
	<xsl:template match="/">
		<html lang="nl-NL">
			<xsl:call-template name="header">
				<xsl:with-param name="css">showLogging.css</xsl:with-param>
				<xsl:with-param name="title"><xsl:value-of select="/page/applicationConstants/properties/property[@name='instance.name']"/> - Browse a queue or topic</xsl:with-param>
			</xsl:call-template>
			<body>
				<xsl:call-template name="menu">
					<xsl:with-param name="environment"><xsl:value-of select="/page/applicationConstants/properties/property[@name='otap.stage']"/></xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="modal">
					<xsl:with-param name="version"><xsl:value-of select="/page/applicationConstants/properties/property[@name='instance.name']"/> ??? ????????-????, IAF <xsl:value-of select="/page/applicationConstants/properties/property[@name='application.version']"/>, buildscript ??, size: ??</xsl:with-param>
					<xsl:with-param name="server">running on ??? using ???</xsl:with-param>
					<xsl:with-param name="heap">heap size: ??M, total JVM memory: ??M</xsl:with-param>
					<xsl:with-param name="space">free space: ??GB, total space: ??GB</xsl:with-param>
				</xsl:call-template>
				<div class="panel panel-primary">
					<div class="panel-heading">
						<h3 class="panel-title">Browse a queue or topic</h3>
					</div>
					<div class="panel-body">
						<form class="form-horizontal">
							<fieldset>
								<legend>Browse queue message form</legend>
									<div class="form-group">
										<label for="select-jdbc" class="control-label col-lg-2">Select a jms realm</label>
										<div class="col-lg-3">
											<select id="select-jdbc" class="form-control">
												<option>JDBC</option>
											</select>
										</div>	
									</div>	
									<div class="form-group">
										<label for="dest-name" class="control-label col-lg-2">Destination name</label>
										<div class="col-lg-3">
											<input id="dest-name" type="text" class="form-control" />
										</div>	
									</div>
									<div class="form-group">
										<label for="select-type" class="control-label col-lg-2">Destination Type (QUEUE or TOPIC)</label>
										<div class="col-lg-3">
											<select id="select-type" class="form-control">
												<option>QUEUE</option>
												<option>TOPIC</option>
											</select>
										</div>	
									</div>
									<div class="form-group">
										<label for="nbr-msg" class="control-label col-lg-2">Number of messages only</label>
										<div class="col-lg-3">
											<input id="nbr-msg" type="checkbox" class="form-control" />								
										</div>
									</div>
									<div class="form-group">
										<label for="show-playload" class="control-label col-lg-2">Show payload</label>
										<div class="col-lg-3">
											<input id="show-playload" type="checkbox" class="form-control" />								
										</div>
									</div>
							</fieldset>
							<fieldset>
								<legend>Action buttons</legend>
									<div class="form-group">
										<div class="col-lg-10 col-lg-offset-2">
											<button class="btn btn-default">Reset</button>
											<button class="btn btn-default">Cancel</button>
											<button class="btn btn-primary" type="submit">Browse</button>
										</div>
									</div>
							</fieldset>
						</form>
					</div>
				</div>
				<script>
					var sd = "2013-11-07 10:02:27.320";
				</script>
				<xsl:call-template name="footer"/>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>
