<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" doctype-system="about:legacy-compat" />
	<xsl:include href="functions.xsl"/>
	<xsl:include href="components.xsl"/>
	<xsl:template match="/">
		<html lang="nl-NL">
			<xsl:call-template name="header">
				<xsl:with-param name="css">showLogging.css</xsl:with-param>
				<xsl:with-param name="title"><xsl:value-of select="/page/applicationConstants/properties/property[@name='instance.name']"/> - Send a JMS message</xsl:with-param>
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
						<h3 class="panel-title">Send a JMS message</h3>
					</div>
					<div class="panel-body">
						<form class="form-horizontal">
							<fieldset>
								<legend>JMS message form</legend>
									<div class="form-group">
										<label class="col-lg-2 control-label" for="select-jms">Select a jms realm </label>
										<div class="col-lg-3"> 
											<select id="select-jms" class="form-control" >
												<option>jdbc</option>
											</select>
										</div>
									</div>
									<div class="form-group">
										<label for="dest-nm" class="control-label col-lg-2">Destination name</label>
										<div class="col-lg-3">
											<input id="dest-nm" type="text" class="form-control" />
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
										<label for="reply" class="control-label col-lg-2">ReplyTo</label>
										<div class="col-lg-3">
											<input id="reply" type="text" class="form-control" />
										</div>	
									</div>
									<div class="form-group">
										<label for="pers" class="control-label col-lg-2">Persistent</label>
										<div class="col-lg-3">
											<input id="pers" type="checkbox" class="form-control" />								
										</div>
									</div>
									<div class="form-group">
										<label for="textArea-msg" class="control-label col-lg-2">Message</label>
										<div class="col-lg-3">
											<textarea id="textArea-msg" class="form-control" rows="3"></textarea>
										</div>	
									</div>
									<div class="form-group">
										<label class="control-label col-lg-2">Select File</label>
										<div class="col-lg-3">
											<input type="file"></input>
										</div>	
									</div>
							</fieldset>
							<fieldset>
								<legend>Action buttons</legend>
									<div class="form-group">
										<div class="col-lg-10 col-lg-offset-2">
											<button class="btn btn-default">Reset</button>
											<button class="btn btn-default">Cancel</button>
											<button class="btn btn-primary" type="submit">Send</button>
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
