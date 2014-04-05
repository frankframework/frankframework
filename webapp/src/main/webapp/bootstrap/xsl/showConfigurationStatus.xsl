<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" doctype-system="about:legacy-compat" />
	<xsl:include href="functions.xsl"/>
	<xsl:include href="components.xsl"/>
	<xsl:template match="/">				
		<html lang="nl-NL">
			<xsl:call-template name="header">
				<xsl:with-param name="css">showConfigurationStatus.css</xsl:with-param>
				<xsl:with-param name="title">IJA_IPLnL 186 20131029-1413 - show ConfigurationStatus</xsl:with-param>
			</xsl:call-template>
			<body>
				<xsl:call-template name="menu">
					<xsl:with-param name="environment">PRD</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="modal">
					<xsl:with-param name="version">IJA_IPLnL 186 20131029-1413, IAF 5.0-a27.3, buildscript 11g, size: 5.1</xsl:with-param>
					
					<xsl:with-param name="server">running on LPAB00000001894 using Apache Tomcat/7.0.22</xsl:with-param>
					<xsl:with-param name="heap">heap size: 89M, total JVM memory: 150M</xsl:with-param>
					<xsl:with-param name="space">free space: 68GB, total space: 74GB</xsl:with-param>
				</xsl:call-template>
				<div class="panel panel-primary">
					<div class="panel-heading">
						<h3 class="panel-title">Summary</h3>
					</div>
					<div class="panel-body">
						<div class="summary summary-left">
							<table class="table table-bordered">
								<tr>
									<th class="table-header">State</th>
									<td class="table-header">
										<img src="bootstrap/img/started.gif" alt="started"/>
									</td>
									<td class="table-header">
										<img src="bootstrap/img/starting.gif" alt="starting"/>
									</td>
									<td class="table-header">
										<img src="bootstrap/img/stopped.gif" alt="stopped"/>
									</td>
									<td class="table-header">
										<img src="bootstrap/img/stopping.gif" alt="stopping"/>
									</td>
									<td class="table-header">
										<img src="bootstrap/img/error.gif" alt="error"/>
									</td>
									<th class="td table-header">
										Actions
									</th>
								</tr>
								<tr>
									<td style="text-align:left;">Adapters</td>
									<td>
										<xsl:value-of select="page/attribute[@name = 'adapters']/registeredAdapters/summary/adapterState/@started"/>
									</td>
									<td>
										<xsl:value-of select="page/attribute[@name = 'adapters']/registeredAdapters/summary/adapterState/@starting"/>
									</td>
									<td>
										<xsl:value-of select="page/attribute[@name = 'adapters']/registeredAdapters/summary/adapterState/@stopped"/>
									</td>
									<td>
										<xsl:value-of select="page/attribute[@name = 'adapters']/registeredAdapters/summary/adapterState/@stopping"/>
									</td>
									<td>
										<xsl:value-of select="page/attribute[@name = 'adapters']/registeredAdapters/summary/adapterState/@error"/>
									</td>
									<td>
										<a title="Stop all adapters">
											<xsl:attribute name="href"><xsl:value-of select="concat('adapterHandler.do?action', '=', 'stopadapter', '&amp;', 'adapterName', '=**ALL**')"/></xsl:attribute>
											<img src="bootstrap/img/stop.gif" alt="Stop all adapters"/>
										</a>
										<a title="Start all adapters">
											<xsl:attribute name="href"><xsl:value-of select="concat('adapterHandler.do?action', '=', 'startadapter', '&amp;', 'adapterName', '=**ALL**')"/></xsl:attribute>
											<img src="bootstrap/img/start.gif" alt="Start all adapters"/>
										</a>
										<a href="images/flow/IBIS.svg" rel="external" title="Show adapter references">
											<img src="bootstrap/img/flow.gif" alt="Show adapter references"/>
										</a>
									</td>
								</tr>
								<tr>
									<td style="text-align:left;">Receivers</td>
									<td>
										<xsl:value-of select="page/attribute[@name = 'adapters']/registeredAdapters/summary/receiverState/@started"/>
									</td>
									<td>
										<xsl:value-of select="page/attribute[@name = 'adapters']/registeredAdapters/summary/receiverState/@starting"/>
									</td>
									<td>
										<xsl:value-of select="page/attribute[@name = 'adapters']/registeredAdapters/summary/receiverState/@stopped"/>
									</td>
									<td>
										<xsl:value-of select="page/attribute[@name = 'adapters']/registeredAdapters/summary/receiverState/@stopping"/>
									</td>
									<td>
										<xsl:value-of select="page/attribute[@name = 'adapters']/registeredAdapters/summary/receiverState/@error"/>
									</td>
								</tr>
							</table>
						</div>
						<div class="summary summary-right">
							<table class="table table-bordered">
								<tr>
									<th class="table-header">Level</th>
									<th class="table-header">INFO</th>
									<th class="table-header">WARN</th>
									<th class="table-header">ERROR</th>
								</tr>
								<tr>
									<td style="text-align:left;">Messages</td>
									<td>
										<xsl:value-of select="page/attribute[@name = 'adapters']/registeredAdapters/summary/messageLevel/@info"/>
									</td>
									<td>
										<xsl:value-of select="page/attribute[@name = 'adapters']/registeredAdapters/summary/messageLevel/@warn"/>
									</td>
									<td>
										<xsl:value-of select="page/attribute[@name = 'adapters']/registeredAdapters/summary/messageLevel/@error"/>
									</td>
								</tr>
							</table>
						</div>
					</div>
				</div>
				<xsl:if test="count(page/attribute/registeredAdapters/exceptions/exception) &gt; 0">
					<div class="alert alert-danger">
						<h4>Exception!</h4>
						<p>
							<xsl:for-each select="page/attribute/registeredAdapters/exceptions/exception">
								<xsl:value-of select="."/>
								<br/>
							</xsl:for-each>
						</p>
					</div>
				</xsl:if>
				<xsl:if test="count(page/attribute/registeredAdapters/warnings/warning[@severe = 'true']) &gt; 0">
					<div class="alert alert-warning">
						<h4>Warning!</h4>
						<p>
							<xsl:for-each select="page/attribute/registeredAdapters/warnings/warning[@severe = 'true']">
								<xsl:value-of select="."/>
								<br/>
							</xsl:for-each>
						</p>
					</div>
				</xsl:if>
				<xsl:if test="count(page/attribute/registeredAdapters/warnings/warning[not(@severe)]) &gt; 0">
					<div class="alert alert-info">
						<h4>Warning!</h4>
						<p>
							<xsl:for-each select="page/attribute/registeredAdapters/warnings/warning[not(@severe)]">
								<xsl:value-of select="."/>
								<br/>
							</xsl:for-each>
						</p>
					</div>
				</xsl:if>
				<xsl:for-each select="page/attribute[@name = 'adapters']/registeredAdapters/adapter">
					<xsl:sort select="@name"/>
					<div class="panel panel-primary">
						<div class="panel-heading">
							<h3 class="panel-title">
								<xsl:value-of select="@name"/>
							</h3>
						</div>
						<div class="panel-body">
							<table class="table table-bordered">
								<tr>
									<th class="table-header">State</th>
									<th class="table-header">Configured</th>
									<th class="table-header">Up since</th>
									<th class="table-header">Messages processed/in process/with error</th>
									<th class="table-header">Actions</th>
								</tr>
								<tr>
									<td>
										<xsl:variable name="adapterstate">
											<xsl:call-template name="lowercase">
												<xsl:with-param name="string" select="@state"/>
											</xsl:call-template>
										</xsl:variable>
										<img>
											<xsl:attribute name="src"><xsl:value-of select="concat('bootstrap/img/', $adapterstate, '.gif')"/></xsl:attribute>
											<xsl:attribute name="alt"><xsl:value-of select="$adapterstate"/></xsl:attribute>
										</img>
									</td>
									<td>
										<xsl:choose>
											<xsl:when test="@configured = 'true'">
												<img src="bootstrap/img/check.gif" alt=""/>
											</xsl:when>
											<xsl:otherwise>
												<img src="bootstrap/img/delete.gif" alt=""/>
											</xsl:otherwise>
										</xsl:choose>
									</td>
									<td>
										<xsl:value-of select="concat(substring(@upSince, 9, 2), '-', substring(@upSince, 6, 2), '-', substring(@upSince, 1, 4), ' ', substring(@upSince, 11, 13))"/>
									</td>
									<td>
										<xsl:value-of select="concat(@messagesProcessed, ' / ', @messagesInProcess, ' / ', @messagesInError)"/>
									</td>
									<td>
										<xsl:choose>
											<xsl:when test="@started = 'true'">
												<a title="stop adapter">
													<xsl:attribute name="href"><xsl:value-of select="concat('adapterHandler.do?action=stopadapter&amp;adapterName=', @name)"/></xsl:attribute>
													<img src="bootstrap/img/stop.gif" alt="stop adapter"/>
												</a>
											</xsl:when>
											<xsl:otherwise>
												<a title="start adapter">
													<xsl:attribute name="href"><xsl:value-of select="concat('adapterHandler.do?action=startadapter&amp;adapterName=', @name)"/></xsl:attribute>
													<img src="bootstrap/img/start.gif" alt="start adapter"/>
												</a>
											</xsl:otherwise>
										</xsl:choose>
										<a title="show Adapter Statistics">
											<xsl:attribute name="href"><xsl:value-of select="concat('showAdapterStatistics.do?adapterName=', @name)"/></xsl:attribute>
											<img class="actions-img" src="bootstrap/img/statistics.gif" alt="show Adapter Statistics"/>
										</a>
										<a title="show Flow Diagram" rel="external">
											<xsl:attribute name="href"><xsl:value-of select="concat('images/flow/', @name, '.svg')"/></xsl:attribute>
											<img class="actions-img" src="bootstrap/img/flow.gif" alt="show Flow Diagram"/>
										</a>
									</td>
								</tr>
								<tr>
									<th class="table-header">State</th>
									<th class="table-header">Receiver name</th>
									<th class="table-header">Listener/sender</th>
									<th class="table-header">Messages received/retried/rejected</th>
									<th class="table-header">Actions</th>
								</tr>
								<xsl:for-each select="receivers/receiver">
									<xsl:sort select="@name"/>
									<tr>
										<td>
											<xsl:variable name="receiverstate">
												<xsl:call-template name="lowercase">
													<xsl:with-param name="string" select="@state"/>
												</xsl:call-template>
											</xsl:variable>
											<img>
												<xsl:attribute name="src"><xsl:value-of select="concat('bootstrap/img/', $receiverstate, '.gif')"/></xsl:attribute>
												<xsl:attribute name="alt"><xsl:value-of select="$receiverstate"/></xsl:attribute>
											</img>
										</td>
										<td>
											<xsl:value-of select="@name"/>
										</td>
										<td>
											<xsl:value-of select="concat(@listenerClass, ' (', @listenerDestination, ')')"/>
										</td>
										<td>
											<xsl:value-of select="concat(@messagesReceived, ' / ', @messagesRetried, ' / ', @messagesRejected)"/>
										</td>
										<td class="actions">
											<xsl:choose>
												<xsl:when test="@isStarted = 'true'">
													<a title="stop receiver">
														<xsl:attribute name="href"><xsl:value-of select="concat('adapterHandler.do?action=stopreceiver&amp;adapterName=', ../../@name ,'&amp;receiverName=', @name)"/></xsl:attribute>
														<img src="bootstrap/img/stop.gif" alt="stop receiver"/>
													</a>
												</xsl:when>
												<xsl:otherwise>
													<a title="start receiver">
														<xsl:attribute name="href"><xsl:value-of select="concat('adapterHandler.do?action=startpreceiver&amp;adapterName=', ../../@name ,'&amp;receiverName=', @name)"/></xsl:attribute>
														<img src="bootstrap/img/start.gif" alt="start receiver"/>
													</a>
												</xsl:otherwise>
											</xsl:choose>
											<xsl:if test="@hasErrorStorage = 'true'">
												<a title="show contents of errorQueue">
													<xsl:attribute name="href"><xsl:value-of select="concat('browser.do?storageType=errorlog&amp;action=show&amp;adapterName=', ../../@name ,'&amp;receiverName=', @name)"/></xsl:attribute>
													<img class="actions-img" src="bootstrap/img/browseErrorStore.gif" alt="show contents of errorQueue"/>
												</a>
												<xsl:value-of select="concat('(', @errorStorageCount ,')')"/>
											</xsl:if>
											<xsl:if test="@hasMessageLog = 'true'">
												<a title="show contents of messageLog">
													<xsl:attribute name="href"><xsl:value-of select="concat('browser.do?storageType=messagelog&amp;action=show&amp;adapterName=', ../../@name ,'&amp;receiverName=', @name)"/></xsl:attribute>
													<img class="actions-img" src="bootstrap/img/browseMessageLog.gif" alt="show contents of messageLog"/>
												</a>
												<xsl:value-of select="concat('(', @messageLogCount ,')')"/>
											</xsl:if>
										</td>
									</tr>
								</xsl:for-each>
								<tr>
									<th colspan="2" class="table-header">Message sending pipe</th>
									<th colspan="2" class="table-header">Sender / listener</th>
									<th class="table-header">Log</th>
								</tr>
								<xsl:for-each select="pipes/pipe">
									<xsl:sort select="@name"/>
									<tr>
										<td colspan="2">
											<xsl:value-of select="@name"/>
										</td>
										<td colspan="2">
											<xsl:value-of select="concat(@sender, ' (', @destination, ')')"/>
										</td>
										<td>
											<xsl:if test="@hasMessageLog = 'true'">
												<a title="show contents of messageLog">
													<xsl:attribute name="href"><xsl:value-of select="concat('browser.do?storageType=messagelog&amp;action=show&amp;adapterName=', ../../@name ,'&amp;pipeName=', @name)"/></xsl:attribute>
													<img class="actions-img" src="bootstrap/img/browseMessageLog.gif" alt="show contents of messageLog"/>
												</a>
												<xsl:value-of select="concat('(', browser/@count ,')')"/>
											</xsl:if>
										</td>
									</tr>
								</xsl:for-each>
								<tr>
									<th colspan="6" class="table-header">Messages</th>
								</tr>
								<tr>
									<td colspan="6">
										<xsl:for-each select="adapterMessages/adapterMessage">
											<p>
												<xsl:choose>
													<xsl:when test="@level = 'ERROR'">
														<xsl:attribute name="class">error</xsl:attribute>
													</xsl:when>
												</xsl:choose>
												<xsl:value-of select="concat(substring(@date, 9, 2), '-', substring(@date, 6, 2), '-', substring(@date, 1, 4), ' ', substring(@date, 11, 13), ' : ', .)"/>
											</p>
										</xsl:for-each>
									</td>
								</tr>
							</table>
						</div>
					</div>
				</xsl:for-each>
				<script>
					var sd = "2013-11-07 10:02:27.320";
				</script>
				<xsl:call-template name="footer"/>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>
