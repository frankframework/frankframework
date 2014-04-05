<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:include href="functions.xsl"/>
	<xsl:include href="components.xsl"/>
	<xsl:template match="/">
		<html lang="nl-NL">
			<xsl:call-template name="header">
				<xsl:with-param name="css">showLogging.css</xsl:with-param>
				<xsl:with-param name="title">IJA_IPLnL 186 20131029-1413 - Test a PipeLine</xsl:with-param>
			</xsl:call-template>
			<body>
				<xsl:call-template name="menu">
					<xsl:with-param name="environment">TST</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="modal">
					<xsl:with-param name="version">IJA_IPLnL 186 20131029-1413, IAF 5.0-a27.3, buildscript 11g, size: 5.1</xsl:with-param>
					<xsl:with-param name="server">running on LPAB00000001894 using Apache Tomcat/7.0.22</xsl:with-param>
					<xsl:with-param name="heap">heap size: 89M, total JVM memory: 150M</xsl:with-param>
					<xsl:with-param name="space">free space: 68GB, total space: 74GB</xsl:with-param>
				</xsl:call-template>
				<div class="panel panel-primary">
					<div class="panel-heading">
						<h3 class="panel-title">Test a PipeLine</h3>
					</div>
					<div class="panel-body">
						<form class="form-horizontal">
							<fieldset>
								<legend>JMS message form</legend>
									<div class="form-group">
										<label class="col-lg-2 control-label" for="select-adapter">Select an adapter</label>
										<div class="col-lg-3"> 
											<select id="select-adapter" class="form-control" >
												<option>-- select an adapter --</option>												
												<option>HelloWorld</option>
												<option>ManageDatabase</option>
											</select>
										</div>
									</div>
									<div class="form-group">
										<label for="textArea-msg" class="control-label col-lg-2">Message</label>
										<div class="col-lg-3">
											<textarea id="textArea-msg" class="form-control" rows="3"></textarea>
										</div>	
									</div>									
									<div class="form-group">
										<label class="control-label col-lg-2">Upload a File</label>
										<div class="col-lg-3">
											<input type="file"></input>											
										</div>	
									</div>
									<div class="form-group">
										<label for="enc" class="control-label col-lg-2">Encoding</label>
										<div class="col-lg-3">
											<input id="enc" type="text" class="form-control" />
										</div>
									</div>	
									<div class="form-group">
										<label for="textArea-result" class="control-label col-lg-2">Result</label>
										<div class="col-lg-3">
											<textarea id="textArea-result" class="form-control" rows="3"></textarea>
										</div>	
									</div>									
									<div class="form-group">
										<label for="state" class="control-label col-lg-2">State</label>
										<div class="col-lg-3">
											<input id="state" type="text" class="form-control" />
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
