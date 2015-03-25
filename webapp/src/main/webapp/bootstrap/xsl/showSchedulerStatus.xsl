<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" doctype-system="about:legacy-compat" />
	<xsl:include href="functions.xsl"/>
	<xsl:include href="components.xsl"/>
	<xsl:include href="url-encode.xsl"/>
	<xsl:template match="/">
		<html lang="nl-NL">
			<xsl:call-template name="header">
				<xsl:with-param name="css">showLogging.css</xsl:with-param>
				<xsl:with-param name="title"><xsl:value-of select="/page/applicationConstants/properties/property[@name='instance.name']"/> - Show Scheduler Status</xsl:with-param>
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
						<h3 class="panel-title">Configuration</h3>
					</div>					
					
					<xsl:for-each select="page/attribute[@name='metadata']/schedulerMetaData">	
					<div class="panel-body">
						<div class="panel-body">								
							<table class="table table-bordered">
								<caption>Scheduler</caption>
								<tbody>
									<tr>
										<th class="table-header">Parameter</th>
										<th class="table-header">Value</th>
										<th class="table-header">Action</th>
									</tr>
									<tr>
										<td>Name</td>
										<td><xsl:value-of select="@schedulerName"/></td>
									</tr>
									<tr>
										<td>schedulerInstanceId</td>
										<td><xsl:value-of select="@schedulerInstanceId"/></td></tr>
									<tr>
										<td>isStarted</td>									
										<td>
											<xsl:choose>
												<xsl:when test="@isStarted = 'False'">												
													<img src="bootstrap/img/no.gif"/>																			
												</xsl:when>
												<xsl:otherwise>												
													<img src="bootstrap/img/started.gif"/>																							
												</xsl:otherwise>
											</xsl:choose>
										</td>
									</tr>
									<tr>
										<td>isPaused</td>
										<td>
											<xsl:choose>
												<xsl:when test="@isPaused = 'False'">
													false
												</xsl:when>
												<xsl:otherwise>
													true										
												</xsl:otherwise>
											</xsl:choose>
										</td>
										<td class="actions">
											<xsl:choose>
												<xsl:when test="@isPaused = 'False'">
													<a href="schedulerHandler.do?action=pauseScheduler">	
													<img src="bootstrap/img/start.gif"/>		
													</a>									
												</xsl:when>
												<xsl:otherwise>
													<a href="schedulerHandler.do?action=startScheduler">
													<img src="bootstrap/img/pause.gif"/>	
													</a>											
												</xsl:otherwise>
											</xsl:choose>
										</td>
									</tr>
									<tr>
										<td>runningSince</td><td><xsl:value-of select="@runningSince"/></td>
									</tr>
									<tr>
										<td>numJobsExecuted</td><td><xsl:value-of select="@numJobsExecuted"/></td>
									</tr>
									<tr>
										<td>isSchedulerRemote</td>
										<td>
											<xsl:choose>
												<xsl:when test="@isSchedulerRemote = 'False'">												
													<img src="bootstrap/img/no.gif"/>
												</xsl:when>
												<xsl:otherwise>												
													<img src="bootstrap/img/started.gif"/>																						
												</xsl:otherwise>
											</xsl:choose>										
										</td>
									</tr>
									<tr><td>threadPoolSize</td><td><xsl:value-of select="@threadPoolSize"/></td></tr> 
									<tr><td>schedulerClass</td><td><xsl:value-of select="@schedulerClass"/></td></tr> 
									<tr><td>version</td><td><xsl:value-of select="@version"/></td></tr>
									<tr><td>jobStoreClass</td><td><xsl:value-of select="@jobStoreClass"/></td></tr> 
									<tr>
										<td>jobStoreSupportsPersistence</td>
										<td>
											<xsl:choose>
												<xsl:when test="@jobStoreSupportsPersistence = 'False'">												
													<img src="bootstrap/img/no.gif"/>																			
												</xsl:when>
												<xsl:otherwise>												
													<img src="bootstrap/img/started.gif"/>																							
												</xsl:otherwise>
											</xsl:choose>
										</td>
									</tr>
								</tbody>
							</table>
						</div>
					</div>	
					</xsl:for-each>							
					<xsl:for-each select="page/attribute/jobGroups/jobGroup">
						
						<xsl:for-each select="jobs">
							<table class="table table-bordered">
								<caption>Jobs in jobgroup <xsl:value-of select="../@name"/></caption>
								<tbody>
				
								<xsl:for-each select="job/jobDetail">
									<xsl:variable name="jobName" select="@jobName"/>
									<xsl:variable name="groupName" select="@groupName"/>
									<tr><th class="table-header">Name</th>
										<th class="table-header">description</th>
										<th class="table-header">jobClass</th>
										<th class="table-header">Action</th>
									</tr>
									<tr>
										<td><xsl:value-of select="@jobName"/></td>
										<td><xsl:value-of select="@description"/></td>
										<td><xsl:value-of select="@jobClass"/></td>
										<td>
											<a title="delete job">
												<xsl:attribute name="href">
													<xsl:value-of select="'schedulerHandler.do?action=deleteJob&amp;jobName='"/>
													<xsl:call-template name="url-encode">
														<xsl:with-param name="str"><xsl:value-of select="@jobName"/></xsl:with-param>
													</xsl:call-template>													
													<xsl:value-of select="'&amp;groupName='"/>
													<xsl:call-template name="url-encode">
														<xsl:with-param name="str"><xsl:value-of select="@groupName"/></xsl:with-param>
													</xsl:call-template>													
												</xsl:attribute>		 
												<img src="bootstrap/img/delete.gif" alt="delete adapter"/>
											</a>													
											<a title="start job">
												<xsl:attribute name="href">
													<xsl:value-of select="'schedulerHandler.do?action=triggerJob&amp;jobName='"/>
													<xsl:call-template name="url-encode">
														<xsl:with-param name="str"><xsl:value-of select="@jobName"/></xsl:with-param>
													</xsl:call-template>													
													<xsl:value-of select="'&amp;groupName='"/>
													<xsl:call-template name="url-encode">
														<xsl:with-param name="str"><xsl:value-of select="@groupName"/></xsl:with-param>
													</xsl:call-template>													
												</xsl:attribute>		 	
												<img src="bootstrap/img/start.gif"/>	
											</a>
										</td>
									</tr>
									<xsl:for-each select="../triggersForJob/triggerDetail">
									<tr>
										<td></td>
										<td colspan="3">			
											<table class="table table-bordered">
												<tr>
													<td>Group</td>
													<td><xsl:value-of select="@triggerGroup"/></td>
												</tr>
												<tr>
													<td>StartTime</td>
													<td><xsl:value-of select="@startTime"/></td>
												</tr>
												<tr>
													<td>next fire</td>
													<td><xsl:value-of select="@nextFireTime"/></td>
													<td>cronExpression</td>
													<td><xsl:value-of select="@cronExpression"/></td>
												</tr>
											</table>
										</td>
										<td></td>
									</tr>
									</xsl:for-each> <!-- trigger -->	
								</xsl:for-each> <!-- jobDetail -->
								</tbody>
							</table>
						</xsl:for-each> <!-- job -->
					</xsl:for-each> <!-- jobGroups/jobGroup -->	
				</div>
				<script>
					var sd = "2013-11-07 10:02:27.320";
				</script>
				<xsl:call-template name="footer"/>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>
