<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<xsl:param name="srcPrefix" />
	<xsl:param name="userAgent" />
	<xsl:param name="configurationName" />
	<xsl:param name="classLoaderType" />
	<xsl:variable name="brvbar" select="'&#166;'" />
	<xsl:variable name="allConfigs" select="root/registeredAdapters/@all" />
	<xsl:variable name="alert" select="root/registeredAdapters/@alert" />
	<xsl:variable name="log" select="root/registeredAdapters/@log" />
	<xsl:variable name="encodedConfigName" select="encode-for-uri($configurationName)" />
	<xsl:template match="/">
		<page title="Show configuration status">
			<xsl:attribute name="title"><xsl:value-of select="concat('Show configuration status: ',$configurationName)" /><xsl:if test="string-length($classLoaderType)&gt;0"><xsl:value-of select="concat(' (',$classLoaderType,')')" /></xsl:if></xsl:attribute>
			<xsl:if test="name(*)='error'">
				<font color="red">
					<xsl:value-of select="*" />
				</font>
			</xsl:if>
			<xsl:call-template name="tabs" />
			<div class="tabpanel">
				<xsl:call-template name="exceptionsAndWarnings" />
				<xsl:call-template name="summary" />
				<xsl:for-each select="//root/registeredAdapters">
					<xsl:choose>
						<xsl:when test="$allConfigs='true' or string-length($alert)&gt;0">
							<xsl:call-template name="adapters_all" />
						</xsl:when>
						<xsl:otherwise>
							<xsl:call-template name="adapters_single" />
						</xsl:otherwise>
					</xsl:choose>
				</xsl:for-each>
			</div>
		</page>
	</xsl:template>
	<xsl:template name="tabs">
		<xsl:if test="count(root/configurations/configuration)&gt;1">
			<ul class="tab">
				<xsl:for-each select="root/configurations/configuration">
					<xsl:sort select="@nameUC" />
					<xsl:choose>
						<xsl:when test=".=$configurationName">
							<li class="active">
								<xsl:value-of select="." />
							</li>
						</xsl:when>
						<xsl:otherwise>
							<li>
								<a>
									<xsl:attribute name="href" select="concat($srcPrefix,'rest/showConfigurationStatus?configuration=',encode-for-uri(.))" />
									<xsl:attribute name="alt" select="." />
									<xsl:attribute name="text" select="." />
									<xsl:value-of select="." />
								</a>
							</li>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:for-each>
			</ul>
		</xsl:if>
	</xsl:template>
	<xsl:template name="exceptionsAndWarnings">
		<xsl:for-each select="root/registeredAdapters">
			<xsl:if test="count(exceptions/exception)&gt;0 or count(warnings/warning)&gt;0">
				<contentTable width="100%">
					<tbody>
						<xsl:for-each select="exceptions">
							<xsl:choose>
								<xsl:when test="$allConfigs='true'">
									<tr>
										<subHeader colspan="2" align="center">
											<h7>Exceptions</h7>
										</subHeader>
									</tr>
									<tr>
										<subHeader>Configuration</subHeader>
										<subHeader>Exception</subHeader>
									</tr>
									<xsl:for-each select="exception">
										<tr>
											<td>
												<font color="red">
													<xsl:value-of select="@config" />
												</font>
											</td>
											<td>
												<font color="red">
													<xsl:value-of select="." />
												</font>
											</td>
										</tr>
									</xsl:for-each>
								</xsl:when>
								<xsl:otherwise>
									<tr>
										<subHeader align="center">
											<h7>Exceptions</h7>
										</subHeader>
									</tr>
									<xsl:for-each select="exception">
										<tr>
											<td>
												<font color="red">
													<xsl:value-of select="." />
												</font>
											</td>
										</tr>
									</xsl:for-each>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:for-each>
						<xsl:for-each select="warnings">
							<xsl:choose>
								<xsl:when test="$allConfigs='true'">
									<tr>
										<subHeader colspan="2" align="center">
											<h7>Warnings</h7>
										</subHeader>
									</tr>
									<tr>
										<subHeader>Configuration</subHeader>
										<subHeader>Warning</subHeader>
									</tr>
									<xsl:for-each select="warning">
										<tr>
											<xsl:choose>
												<xsl:when test="@severe='true'">
													<td>
														<font color="red">
															<xsl:value-of select="@config" />
														</font>
													</td>
													<td>
														<font color="red">
															<xsl:value-of select="." />
														</font>
													</td>
												</xsl:when>
												<xsl:otherwise>
													<td>
														<xsl:value-of select="@config" />
													</td>
													<td>
														<xsl:value-of select="." />
													</td>
												</xsl:otherwise>
											</xsl:choose>
										</tr>
									</xsl:for-each>
								</xsl:when>
								<xsl:otherwise>
									<tr>
										<subHeader align="center">
											<h7>Warnings</h7>
										</subHeader>
									</tr>
									<xsl:for-each select="warning">
										<tr>
											<xsl:choose>
												<xsl:when test="@severe='true'">
													<td>
														<font color="red">
															<xsl:value-of select="." />
														</font>
													</td>
												</xsl:when>
												<xsl:otherwise>
													<td>
														<xsl:value-of select="." />
													</td>
												</xsl:otherwise>
											</xsl:choose>
										</tr>
									</xsl:for-each>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:for-each>
					</tbody>
				</contentTable>
			</xsl:if>
		</xsl:for-each>
	</xsl:template>
	<xsl:template name="summary">
		<contentTable width="100%">
			<tbody>
				<tr>
					<subHeader colspan="13" align="center">
						<h7>Summary</h7>
					</subHeader>
				</tr>
				<tr>
					<subHeader rowspan="2">State</subHeader>
					<subHeader rowspan="2" align="center">
						<image type="started" title="started" />
					</subHeader>
					<subHeader rowspan="2" align="center">
						<image type="starting" title="starting" />
					</subHeader>
					<subHeader rowspan="2" align="center">
						<image type="stopped" title="stopped" />
					</subHeader>
					<subHeader rowspan="2" align="center">
						<image type="stopping" title="stopping" />
					</subHeader>
					<subHeader rowspan="2" align="center">
						<image type="error" title="error" />
					</subHeader>
					<subHeader colspan="3" align="center">Last message state</subHeader>
					<subHeader rowspan="2" align="center">INFO messages</subHeader>
					<subHeader rowspan="2" align="center">WARN messages</subHeader>
					<subHeader rowspan="2" align="center">ERROR messages</subHeader>
					<subHeader rowspan="2">Actions</subHeader>
				</tr>
				<tr>
					<subHeader align="center">
						<image type="check" title="ok" />
					</subHeader>
					<subHeader align="center">-</subHeader>
					<subHeader align="center">
						<image type="delete" title="error" />
					</subHeader>
				</tr>
				<xsl:variable name="configFlowUrl" select="concat($srcPrefix,'rest/showFlowDiagram?configuration=',$encodedConfigName)" />
				<tr>
					<subHeader>Adapters</subHeader>
					<td class="receiverRow" align="right">
						<xsl:value-of select="//root/registeredAdapters/summary/adapterState/@started" />
					</td>
					<td class="receiverRow" align="right">
						<xsl:value-of select="//root/registeredAdapters/summary/adapterState/@starting" />
					</td>
					<td class="receiverRow" align="right">
						<xsl:value-of select="//root/registeredAdapters/summary/adapterState/@stopped" />
					</td>
					<td class="receiverRow" align="right">
						<xsl:value-of select="//root/registeredAdapters/summary/adapterState/@stopping" />
					</td>
					<td class="receiverRow" align="right">
						<xsl:value-of select="//root/registeredAdapters/summary/adapterState/@error" />
					</td>
					<td class="receiverRow" align="right">
						<xsl:value-of select="//root/registeredAdapters/summary/lastMsgProcessState/@ok" />
					</td>
					<td class="receiverRow" align="right">
						<xsl:value-of select="//root/registeredAdapters/summary/lastMsgProcessState/@notApplicable" />
					</td>
					<td class="receiverRow" align="right">
						<xsl:value-of select="//root/registeredAdapters/summary/lastMsgProcessState/@error" />
					</td>
					<td rowspan="2" class="receiverRow" align="right">
						<xsl:value-of select="//root/registeredAdapters/summary/messageLevel/@info" />
					</td>
					<td rowspan="2" class="receiverRow" align="right">
						<xsl:value-of select="//root/registeredAdapters/summary/messageLevel/@warn" />
					</td>
					<td rowspan="2" class="receiverRow" align="right">
						<xsl:value-of select="//root/registeredAdapters/summary/messageLevel/@error" />
					</td>
					<td rowspan="2" width="200">
						<imagelink type="stop" alt="stop all adapters">
							<xsl:attribute name="href" select="concat($srcPrefix, 'adapterHandler.do')" />
							<parameter name="action">stopadapter</parameter>
							<parameter name="configurationName">
								<xsl:value-of select="$encodedConfigName" />
							</parameter>
							<parameter name="adapterName">*ALL*</parameter>
						</imagelink>
						<imagelink type="start" alt="start all adapters">
							<xsl:attribute name="href" select="concat($srcPrefix, 'adapterHandler.do')" />
							<parameter name="action">startadapter</parameter>
							<parameter name="configurationName">
								<xsl:value-of select="$encodedConfigName" />
							</parameter>
							<parameter name="adapterName">*ALL*</parameter>
						</imagelink>
						<xsl:choose>
							<xsl:when test="$configurationName='*ALL*'">
								<imagelink type="fullreload" alt="full reload">
									<xsl:attribute name="href" select="concat($srcPrefix, 'adapterHandlerAsAdmin.do')" />
									<parameter name="action">fullreload</parameter>
								</imagelink>
							</xsl:when>
							<xsl:otherwise>
								<imagelink type="reload" alt="reload configuration">
									<xsl:attribute name="href" select="concat($srcPrefix, 'adapterHandler.do')" />
									<parameter name="action">reload</parameter>
									<parameter name="configurationName">
										<xsl:value-of select="$encodedConfigName" />
									</parameter>
								</imagelink>
							</xsl:otherwise>
						</xsl:choose>
						<imagelink type="flow" alt="show adapter references" newwindow="true">
							<xsl:attribute name="href" select="$configFlowUrl" />
						</imagelink>
					</td>
				</tr>
				<tr>
					<subHeader>Receivers</subHeader>
					<td class="receiverRow" align="right">
						<xsl:choose>
							<xsl:when test="number(//root/registeredAdapters/summary/receiverState/@startedNotAvailable)=0">
								<xsl:value-of select="//root/registeredAdapters/summary/receiverState/@started" />
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="concat(//root/registeredAdapters/summary/receiverState/@started,'/',//root/registeredAdapters/summary/receiverState/@startedNotAvailable)" />
							</xsl:otherwise>
						</xsl:choose>
					</td>
					<td class="receiverRow" align="right">
						<xsl:value-of select="//root/registeredAdapters/summary/receiverState/@starting" />
					</td>
					<td class="receiverRow" align="right">
						<xsl:value-of select="//root/registeredAdapters/summary/receiverState/@stopped" />
					</td>
					<td class="receiverRow" align="right">
						<xsl:value-of select="//root/registeredAdapters/summary/receiverState/@stopping" />
					</td>
					<td class="receiverRow" align="right">
						<xsl:value-of select="//root/registeredAdapters/summary/receiverState/@error" />
					</td>
					<td colspan="3" class="receiverRow"/>
				</tr>
				<xsl:for-each select="//root/registeredAdapters/configurationMessages">
					<tr>
						<subHeader colspan="12">Messages</subHeader>
						<subHeader>Flow</subHeader>
					</tr>
					<xsl:variable name="nrOfConfigurationMessages" select="count(configurationMessage)" />
					<xsl:for-each select="configurationMessage">
						<xsl:variable name="posMessages" select="position()" />
						<tr>
							<td colspan="12" class="messagesRow">
								<xsl:choose>
									<xsl:when test="@level='ERROR'">
										<font color="red">
											<xsl:value-of select="@date" />
											<xsl:text> : </xsl:text>
											<xsl:value-of select="." />
										</font>
									</xsl:when>
									<xsl:when test="@level='WARN'">
										<font color="orange">
											<xsl:value-of select="@date" />
											<xsl:text> : </xsl:text>
											<xsl:value-of select="." />
										</font>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="@date" />
										<xsl:text> : </xsl:text>
										<xsl:value-of select="." />
									</xsl:otherwise>
								</xsl:choose>
							</td>
							<xsl:if test="$posMessages=1">
								<xsl:variable name="active">
									<xsl:choose>
										<xsl:when test="contains($userAgent, 'MSIE 8.0') or contains($userAgent, 'MSIE 9.0') or contains($userAgent, 'MSIE 10.')">
											<xsl:text>false</xsl:text>
										</xsl:when>
										<xsl:otherwise>
											<xsl:text>true</xsl:text>
										</xsl:otherwise>
									</xsl:choose>
								</xsl:variable>
								<td width="200">
									<xsl:attribute name="rowspan" select="$nrOfConfigurationMessages" />
									<xsl:if test="$active='true'">
										<xsl:attribute name="bgcolor" select="'white'" />
										<a newwindow="true">
											<xsl:attribute name="href" select="$configFlowUrl" />
											<img title="IBIS adapter references" width="200" height="200">
												<xsl:attribute name="src" select="$configFlowUrl" />
											</img>
										</a>
									</xsl:if>
								</td>
							</xsl:if>
						</tr>
					</xsl:for-each>
				</xsl:for-each>
			</tbody>
		</contentTable>
	</xsl:template>
	<xsl:template name="adapters_all">
		<contentTable width="100%">
			<tbody>
				<tr>
					<subHeader colspan="10" align="center">
						<xsl:variable name="txt_all" select="'*all*'"/>
						<xsl:variable name="txt_only_state_alerts" select="'only state alerts'"/>
						<xsl:variable name="txt_only_log_alerts" select="'only log alerts'"/>
						<xsl:choose>
							<xsl:when test="$alert='true'">
								<xsl:choose>
									<xsl:when test="$log='true'">
										<h7>
											<a name="Adapters"></a>
											<xsl:text>Adapters (only log alerts)</xsl:text>
											<span style="display:inline-block; width: 10px;"></span>
											<a>
												<xsl:attribute name="href" select="concat($srcPrefix,'rest/showConfigurationStatus?alert=false#Adapters')" />
												<xsl:attribute name="alt" select="$txt_all" />
												<xsl:attribute name="text" select="$txt_all" />
												<xsl:value-of select="$txt_all" />
											</a>
											<span style="display:inline-block; width: 10px;"></span>
											<a>
												<xsl:attribute name="href" select="concat($srcPrefix,'rest/showConfigurationStatus?alert=true#Adapters')" />
												<xsl:attribute name="alt" select="$txt_only_state_alerts" />
												<xsl:attribute name="text" select="$txt_only_state_alerts" />
												<xsl:value-of select="$txt_only_state_alerts" />
											</a>
										</h7>
									</xsl:when>
									<xsl:otherwise>
										<h7>
											<a name="Adapters"></a>
											<xsl:text>Adapters (only state alerts)</xsl:text>
											<span style="display:inline-block; width: 10px;"></span>
											<a>
												<xsl:attribute name="href" select="concat($srcPrefix,'rest/showConfigurationStatus?alert=false#Adapters')" />
												<xsl:attribute name="alt" select="$txt_all" />
												<xsl:attribute name="text" select="$txt_all" />
												<xsl:value-of select="$txt_all" />
											</a>
											<span style="display:inline-block; width: 10px;"></span>
											<a>
												<xsl:attribute name="href" select="concat($srcPrefix,'rest/showConfigurationStatus?alert=true&amp;log=true#Adapters')" />
												<xsl:attribute name="alt" select="$txt_only_log_alerts" />
												<xsl:attribute name="text" select="$txt_only_log_alerts" />
												<xsl:value-of select="$txt_only_log_alerts" />
											</a>
										</h7>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:when>
							<xsl:otherwise>	
								<h7>
									<a name="Adapters"></a>
									<xsl:text>Adapters (*all*)</xsl:text>
									<span style="display:inline-block; width: 10px;"></span>
									<a>
										<xsl:attribute name="href" select="concat($srcPrefix,'rest/showConfigurationStatus?alert=true#Adapters')" />
										<xsl:attribute name="alt" select="$txt_only_state_alerts" />
										<xsl:attribute name="text" select="$txt_only_state_alerts" />
										<xsl:value-of select="$txt_only_state_alerts" />
									</a>
									<span style="display:inline-block; width: 10px;"></span>
									<a>
										<xsl:attribute name="href" select="concat($srcPrefix,'rest/showConfigurationStatus?alert=true&amp;log=true#Adapters')" />
										<xsl:attribute name="alt" select="$txt_only_log_alerts" />
										<xsl:attribute name="text" select="$txt_only_log_alerts" />
										<xsl:value-of select="$txt_only_log_alerts" />
									</a>
								</h7>
							</xsl:otherwise>
						</xsl:choose>
					</subHeader>
				</tr>
				<tr>
					<subHeader>Configuration</subHeader>
					<subHeader>Name</subHeader>
					<subHeader>State</subHeader>
					<subHeader>Up since</subHeader>
					<subHeader colspan="2">Last message</subHeader>
					<subHeader>Messages with error</subHeader>
					<subHeader>Messages processed/in process</subHeader>
					<subHeader>State receiver(s)</subHeader>
					<subHeader>Log I/W/E</subHeader>
				</tr>
				<xsl:for-each select="adapter">
					<xsl:sort select="concat(@configUC,'|',@nameUC)" />
					<xsl:if test="not($alert='true') or ($alert='true' and not($log='true') and @stateAlert='true') or ($alert='true' and $log='true' and @logAlert='true')">
						<tr ref="adapterRow">
							<td>
								<xsl:value-of select="@config" />
							</td>
							<td>
								<a>
									<xsl:attribute name="href" select="concat($srcPrefix,'rest/showConfigurationStatus?configuration=',encode-for-uri(@config),'#',@name)" />
									<xsl:value-of select="@name" />
								</a>
							</td>
							<td>
								<xsl:call-template name="runStateImage">
									<xsl:with-param name="state" select="@state" />
								</xsl:call-template>
							</td>
							<td>
								<xsl:value-of select="@upSince" />
								<xsl:if test="@upSinceAge!=''">
									<xsl:value-of select="concat(' (',@upSinceAge,')')" />
								</xsl:if>
							</td>
							<td>
								<xsl:value-of select="@lastMessageDate" />
								<xsl:if test="@lastMessageDateAge!=''">
									<xsl:value-of select="concat(' (',@lastMessageDateAge,')')" />
								</xsl:if>
							</td>
							<td>
								<xsl:choose>
									<xsl:when test="@lastMsgProcessState='OK'">
										<image type="check" title="ok" />
									</xsl:when>
									<xsl:when test="@lastMsgProcessState='ERROR'">
										<image type="delete" title="error" />
									</xsl:when>
									<xsl:otherwise>
									<xsl:value-of select="@lastMsgProcessState" />
									</xsl:otherwise>
								</xsl:choose>
							</td>
							<td align="right">
								<xsl:value-of select="@messagesInError" />
							</td>
							<td align="right">
								<xsl:value-of select="concat(@messagesProcessed,'/',@messagesInProcess)" />
							</td>
							<td>
								<xsl:for-each select="receivers/receiver">
									<xsl:call-template name="runStateImage">
										<xsl:with-param name="state" select="@state" />
										<xsl:with-param name="isAvailable" select="@isAvailable" />
									</xsl:call-template>
								</xsl:for-each>
							</td>
							<td align="right">
								<xsl:value-of select="concat(adapterMessages/@info,'/',adapterMessages/@warn,'/',adapterMessages/@error)" />
							</td>
						</tr>
					</xsl:if>
				</xsl:for-each>
			</tbody>
		</contentTable>
	</xsl:template>
	<xsl:template name="adapters_single">
		<xsl:for-each select="adapter">
			<xsl:sort select="@nameUC" />
			<xsl:variable name="adapterName" select="@name" />
			<xsl:variable name="encodedAdapterName" select="encode-for-uri($adapterName)" />
			<xsl:variable name="adapterFlowUrl" select="concat($srcPrefix,'rest/showFlowDiagram/',$encodedAdapterName)" />
			<contentTable width="100%">
				<tbody>
					<xsl:variable name="adapterState" select="@state" />
					<tr>
						<subHeader colspan="8">
							<h8>
								<a>
									<xsl:attribute name="name" select="@name" />
								</a>
								<xsl:value-of select="@name" />
								<xsl:if test="@description!=''">
									<xsl:value-of select="concat(' (',@description,')')" />
								</xsl:if>
							</h8>
						</subHeader>
					</tr>
					<tr>
						<subHeader>State</subHeader>
						<subHeader>Configured</subHeader>
						<subHeader>Up since</subHeader>
						<subHeader colspan="2">Last message</subHeader>
						<subHeader>Messages with error</subHeader>
						<subHeader>Messages processed/in process</subHeader>
						<subHeader>Actions</subHeader>
					</tr>
					<tr ref="adapterRow">
						<td>
							<xsl:call-template name="runStateImage">
								<xsl:with-param name="state" select="@state" />
							</xsl:call-template>
						</td>
						<td>
							<booleanImage>
								<xsl:attribute name="value" select="@configured" />
							</booleanImage>
						</td>
						<td>
							<xsl:value-of select="@upSince" />
							<xsl:if test="@upSinceAge!=''">
								<xsl:value-of select="concat(' (',@upSinceAge,')')" />
							</xsl:if>
						</td>
						<td>
							<xsl:value-of select="@lastMessageDate" />
							<xsl:if test="@lastMessageDateAge!=''">
								<xsl:value-of select="concat(' (',@lastMessageDateAge,')')" />
							</xsl:if>
						</td>
						<td>
							<xsl:choose>
								<xsl:when test="@lastMsgProcessState='OK'">
									<image type="check" title="ok" />
								</xsl:when>
								<xsl:when test="@lastMsgProcessState='ERROR'">
									<image type="delete" title="error" />
								</xsl:when>
								<xsl:otherwise>
								<xsl:value-of select="@lastMsgProcessState" />
								</xsl:otherwise>
							</xsl:choose>
						</td>
						<td align="right">
							<xsl:value-of select="@messagesInError" />
						</td>
						<td align="right">
							<xsl:value-of select="concat(@messagesProcessed,'/',@messagesInProcess)" />
						</td>
						<td>
							<imagelinkMenu>
								<xsl:if test="@state='Stopped'">
									<imagelink type="start" alt="start">
										<xsl:attribute name="href" select="concat($srcPrefix, 'adapterHandler.do')" />
										<parameter name="action">startadapter</parameter>
										<parameter name="adapterName">
											<xsl:value-of select="$encodedAdapterName" />
										</parameter>
									</imagelink>
								</xsl:if>
								<xsl:if test="@state='Started'">
									<imagelink type="stop" alt="stop">
										<xsl:attribute name="href" select="concat($srcPrefix, 'adapterHandler.do')" />
										<parameter name="action">stopadapter</parameter>
										<parameter name="adapterName">
											<xsl:value-of select="$encodedAdapterName" />
										</parameter>
									</imagelink>
								</xsl:if>
								<imagelink type="adapterStatistics" alt="show Adapter Statistics">
									<xsl:attribute name="href" select="concat($srcPrefix, 'showAdapterStatistics.do')" />
									<parameter name="adapterName">
										<xsl:value-of select="$encodedAdapterName" />
									</parameter>
								</imagelink>
								<imagelink type="flow" alt="show Adapter Flow" newwindow="true">
									<xsl:attribute name="href" select="$adapterFlowUrl" />
								</imagelink>
								<xsl:value-of select="@state" />
							</imagelinkMenu>
						</td>
					</tr>
					<xsl:for-each select="receivers">
						<tr>
							<subHeader>State</subHeader>
							<subHeader colspan="2">Receiver name</subHeader>
							<xsl:choose>
								<xsl:when test="count(receiver[@pendingMessagesCount!=''])!=0">
									<subHeader colspan="2">Listener/Sender</subHeader>
									<subHeader>Messages pending</subHeader>
								</xsl:when>
								<xsl:otherwise>
									<subHeader colspan="3">Listener/Sender</subHeader>
								</xsl:otherwise>
							</xsl:choose>
							<subHeader>Messages received/retried/rejected</subHeader>
							<subHeader>Actions</subHeader>
						</tr>
						<xsl:for-each select="receiver">
							<xsl:variable name="encodedReceiverName" select="encode-for-uri(@name)" />
							<xsl:variable name="receiverState" select="@state" />
							<xsl:variable name="receiverName" select="@name" />
							<xsl:variable name="hasInprocessStorage" select="@hasInprocessStorage" />
							<xsl:variable name="hasErrorStorage" select="@hasErrorStorage" />
							<xsl:variable name="hasMessageLog" select="@hasMessageLog" />
							<xsl:variable name="isRestListener" select="@isRestListener" />
							<xsl:variable name="restUriPattern" select="@restUriPattern" />
							<xsl:variable name="isView" select="@isView" />
							<xsl:variable name="isEsbJmsFFListener" select="@isEsbJmsFFListener" />
							<tr>
								<td class="receiverRow">
									<xsl:call-template name="runStateImage">
										<xsl:with-param name="state" select="@state" />
										<xsl:with-param name="isAvailable" select="@isAvailable" />
									</xsl:call-template>
								</td>
								<td colspan="2" class="receiverRow">
									<xsl:value-of select="@name" />
								</td>
								<xsl:choose>
									<xsl:when test="@pendingMessagesCount!=''">
										<td colspan="2" class="receiverRow">
											<xsl:call-template name="receiverInfo">
												<xsl:with-param name="encodedAdapterName" select="$encodedAdapterName" />
												<xsl:with-param name="encodedReceiverName" select="$encodedReceiverName" />
											</xsl:call-template>
										</td>
										<td class="receiverRow" align="right">
											<xsl:choose>
												<xsl:when test="@esbPendingMessagesCount!=''">
													<xsl:value-of select="concat(@pendingMessagesCount,'/',@esbPendingMessagesCount)" />
												</xsl:when>
												<xsl:otherwise>
													<xsl:value-of select="@pendingMessagesCount" />
												</xsl:otherwise>
											</xsl:choose>
										</td>
									</xsl:when>
									<xsl:otherwise>
										<td colspan="3" class="receiverRow">
											<xsl:call-template name="receiverInfo">
												<xsl:with-param name="encodedAdapterName" select="$encodedAdapterName" />
												<xsl:with-param name="encodedReceiverName" select="$encodedReceiverName" />
											</xsl:call-template>
										</td>
									</xsl:otherwise>
								</xsl:choose>
								<td class="receiverRow" align="right">
									<xsl:value-of select="concat(@messagesReceived,'/',@messagesRetried,'/',@messagesRejected)" />
								</td>
								<td nowrap="true" width="200">
									<xsl:if test="@state='Stopped'">
										<imagelink type="start" alt="start receiver">
											<xsl:attribute name="href" select="concat($srcPrefix, 'adapterHandler.do')" />
											<parameter name="action">startreceiver</parameter>
											<parameter name="adapterName">
												<xsl:value-of select="$encodedAdapterName" />
											</parameter>
											<parameter name="receiverName">
												<xsl:value-of select="$encodedReceiverName" />
											</parameter>
										</imagelink>
									</xsl:if>
									<xsl:if test="@state='Started'">
										<imagelink type="stop" alt="stop receiver">
											<xsl:attribute name="href" select="concat($srcPrefix, 'adapterHandler.do')" />
											<parameter name="action">stopreceiver</parameter>
											<parameter name="adapterName">
												<xsl:value-of select="$encodedAdapterName" />
											</parameter>
											<parameter name="receiverName">
												<xsl:value-of select="$encodedReceiverName" />
											</parameter>
										</imagelink>
									</xsl:if>
									<xsl:if test="@isRestListener='true' and @isView='true'">
										<imagelink type="showashtml">
											<xsl:attribute name="href" select="concat($srcPrefix, @restUriPattern)" />
											<xsl:attribute name="alt" select="@name" />
										</imagelink>
									</xsl:if>
									<xsl:if test="@hasErrorStorage='true'">
										<xsl:if test="@isEsbJmsFFListener='true'">
											<imagelink type="move" alt="move message">
												<xsl:attribute name="href" select="concat($srcPrefix, 'adapterHandler.do')" />
												<parameter name="action">type="move" alt="move message"</parameter>
												<parameter name="adapterName">
													<xsl:value-of select="$encodedAdapterName" />
												</parameter>
												<parameter name="receiverName">
													<xsl:value-of select="$encodedReceiverName" />
												</parameter>
											</imagelink>
										</xsl:if>
										<xsl:choose>
											<xsl:when test="@errorStorageCount='-1'">
												<image type="browseErrorStore" alt="errorStore" />
											</xsl:when>
											<xsl:otherwise>
												<imagelink type="browseErrorStore" alt="show contents of errorQueue">
													<xsl:attribute name="href" select="concat($srcPrefix, 'browser.do')" />
													<parameter name="storageType">errorlog</parameter>
													<parameter name="action">show</parameter>
													<parameter name="adapterName">
														<xsl:value-of select="$encodedAdapterName" />
													</parameter>
													<parameter name="receiverName">
														<xsl:value-of select="$encodedReceiverName" />
													</parameter>
												</imagelink>
												<xsl:value-of select="concat(' (',@errorStorageCount,')')" />
											</xsl:otherwise>
										</xsl:choose>
									</xsl:if>
									<xsl:if test="@hasMessageLog='true'">
										<xsl:choose>
											<xsl:when test="@messageLogCount='-1'">
												<image type="browseMessageLog" alt="messageLog" />
											</xsl:when>
											<xsl:otherwise>
												<imagelink type="browseMessageLog" alt="show contents of messageLog">
													<xsl:attribute name="href" select="concat($srcPrefix, 'browser.do')" />
													<parameter name="storageType">messagelog</parameter>
													<parameter name="action">show</parameter>
													<parameter name="adapterName">
														<xsl:value-of select="$encodedAdapterName" />
													</parameter>
													<parameter name="receiverName">
														<xsl:value-of select="$encodedReceiverName" />
													</parameter>
												</imagelink>
												<xsl:value-of select="concat(' (',@messageLogCount,')')" />
											</xsl:otherwise>
										</xsl:choose>
									</xsl:if>
								</td>
							</tr>
						</xsl:for-each>
					</xsl:for-each>
					<xsl:if test="count(pipes/pipe[@sender!='' and (not(@isJdbcSender='true') or @hasMessageLog='true')])!=0">
						<tr>
							<subHeader colspan="3">Message sending pipes</subHeader>
							<subHeader colspan="4">Sender/Listener</subHeader>
							<subHeader>Show log</subHeader>
						</tr>
						<xsl:for-each select="pipes/pipe[@sender!='' and (not(@isJdbcSender='true') or @hasMessageLog='true')]">
							<xsl:variable name="encodedPipeName" select="encode-for-uri(@name)" />
							<xsl:variable name="hasMessageLog" select="@hasMessageLog" />
							<xsl:variable name="messageLogCount" select="@messageLogCount" />
							<tr>
								<td colspan="3" class="receiverRow">
									<xsl:value-of select="@name" />
								</td>
								<td colspan="4" class="receiverRow">
									<xsl:value-of select="@sender" />
									<xsl:if test="@destination!=''">
										<xsl:value-of select="concat(' (',@destination,')')" />
									</xsl:if>
									<xsl:if test="@listenerClass!=''">
										<xsl:value-of select="concat(' /',@listenerClass)" />
										<xsl:if test="@listenerDestination!=''">
											<xsl:value-of select="concat(' (',@listenerDestination,')')" />
										</xsl:if>
									</xsl:if>
								</td>
								<td>
									<xsl:if test="@hasMessageLog='true'">
										<xsl:choose>
											<xsl:when test="@messageLogCount='-1'">
												<image type="browseMessageLog" alt="messageLog" />
											</xsl:when>
											<xsl:otherwise>
												<imagelink type="browseMessageLog" alt="show contents of messageLog">
													<xsl:attribute name="href" select="concat($srcPrefix, 'browser.do')" />
													<parameter name="storageType">messagelog</parameter>
													<parameter name="action">show</parameter>
													<parameter name="adapterName">
														<xsl:value-of select="$encodedAdapterName" />
													</parameter>
													<parameter name="pipeName">
														<xsl:value-of select="$encodedPipeName" />
													</parameter>
												</imagelink>
												<xsl:value-of select="concat(' (',@messageLogCount,')')" />
											</xsl:otherwise>
										</xsl:choose>
									</xsl:if>
								</td>
							</tr>
						</xsl:for-each>
					</xsl:if>
					<xsl:for-each select="adapterMessages">
						<tr>
							<subHeader colspan="7">Messages</subHeader>
							<subHeader>Flow</subHeader>
						</tr>
						<xsl:variable name="nrOfAdapterMessages" select="count(adapterMessage)" />
						<xsl:for-each select="adapterMessage">
							<xsl:variable name="posMessages" select="position()" />
							<tr>
								<td colspan="7" class="messagesRow">
									<xsl:choose>
										<xsl:when test="@level='ERROR'">
											<font color="red">
												<xsl:value-of select="@date" />
												<xsl:text> : </xsl:text>
												<xsl:value-of select="." />
											</font>
										</xsl:when>
										<xsl:when test="@level='WARN'">
											<font color="orange">
												<xsl:value-of select="@date" />
												<xsl:text> : </xsl:text>
												<xsl:value-of select="." />
											</font>
										</xsl:when>
										<xsl:otherwise>
											<xsl:value-of select="@date" />
											<xsl:text> : </xsl:text>
											<xsl:value-of select="." />
										</xsl:otherwise>
									</xsl:choose>
								</td>
								<xsl:if test="$posMessages=1">
									<xsl:variable name="active">
										<xsl:choose>
											<xsl:when test="contains($userAgent, 'MSIE 8.0') or contains($userAgent, 'MSIE 9.0') or contains($userAgent, 'MSIE 10.')">
												<xsl:text>false</xsl:text>
											</xsl:when>
											<xsl:otherwise>
												<xsl:text>true</xsl:text>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:variable>
									<td width="200">
										<xsl:attribute name="rowspan" select="$nrOfAdapterMessages" />
										<xsl:if test="$active='true'">
											<xsl:attribute name="bgcolor" select="'white'" />
											<a newwindow="true">
												<xsl:attribute name="href" select="$adapterFlowUrl" />
												<img width="200" height="200">
													<xsl:attribute name="src" select="$adapterFlowUrl" />
													<xsl:attribute name="title" select="$adapterName" />
												</img>
											</a>
										</xsl:if>
									</td>
								</xsl:if>
							</tr>
						</xsl:for-each>
					</xsl:for-each>
				</tbody>
			</contentTable>
		</xsl:for-each>
	</xsl:template>
	<xsl:template name="runStateImage">
		<xsl:param name="state" />
		<xsl:param name="isAvailable" />
		<xsl:choose>
			<xsl:when test="$state='Stopped'">
				<image type="stopped" title="stopped" />
			</xsl:when>
			<xsl:when test="$state='Started'">
				<xsl:choose>
					<xsl:when test="$isAvailable='false'">
						<image type="no" title="started_not_available" />
					</xsl:when>
					<xsl:otherwise>
						<image type="started" title="started" />
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:when test="$state='**ERROR**'">
				<image type="error" title="error" />
			</xsl:when>
			<xsl:when test="$state='Stopping'">
				<image type="stopping" title="stopping" />
			</xsl:when>
			<xsl:when test="$state='Starting'">
				<image type="starting" title="starting" />
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	<xsl:template name="receiverInfo">
		<xsl:param name="encodedAdapterName" />
		<xsl:param name="encodedReceiverName" />
		<xsl:value-of select="@listenerClass" />
		<xsl:if test="@listenerDestination!=''">
			<xsl:value-of select="concat(' (',@listenerDestination,')')" />
		</xsl:if>
		<xsl:if test="@senderClass!=''">
			<xsl:value-of select="concat(' / ',@senderClass)" />
			<xsl:if test="@senderDestination!=''">
				<xsl:value-of select="concat(' (',@senderDestination,')')" />
			</xsl:if>
		</xsl:if>
		<xsl:if test="@threadCount!=0">
			<br />
			<xsl:value-of select="concat('(',@threadCount,'/',@maxThreadCount,'thread')" />
			<xsl:if test="@maxThreadCount!=1">
				<xsl:text>s</xsl:text>
			</xsl:if>
			<xsl:if test="@threadCountControllable='true'">
				<xsl:text>, </xsl:text>
				<imagelink type="incthreads" alt="increase the maximum number of threads" text="inc">
					<xsl:attribute name="href" select="concat($srcPrefix, 'adapterHandler.do')" />
					<parameter name="action">incthreads</parameter>
					<parameter name="adapterName">
						<xsl:value-of select="$encodedAdapterName" />
					</parameter>
					<parameter name="receiverName">
						<xsl:value-of select="$encodedReceiverName" />
					</parameter>
				</imagelink>
				<xsl:text> - </xsl:text>
				<imagelink type="decthreads" alt="decrease the maximum number of threads" text="dec">
					<xsl:attribute name="href" select="concat($srcPrefix, 'adapterHandler.do')" />
					<parameter name="action">decthreads</parameter>
					<parameter name="adapterName">
						<xsl:value-of select="$encodedAdapterName" />
					</parameter>
					<parameter name="receiverName">
						<xsl:value-of select="$encodedReceiverName" />
					</parameter>
				</imagelink>
			</xsl:if>
			<xsl:text>)</xsl:text>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>
