<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>
<%@ page import="nl.nn.adapterframework.util.RunStateEnum" %>
<%@ page import="nl.nn.adapterframework.util.FileUtils" %>
<%@ page import="nl.nn.adapterframework.util.XmlUtils" %>

<page title="Show configuration status: <% out.write(XmlUtils.encodeChars((String)session.getAttribute("configurationName")));
	String classLoaderType = (String)session.getAttribute("classLoaderType");
	if (classLoaderType!=null) out.write(" ("+classLoaderType+")"); %>"
refresh="showConfigurationStatus.do">

	<xtags:parse>
		<% out.write(XmlUtils.replaceNonValidXmlCharacters(request.getAttribute("configurations").toString())); %>
	</xtags:parse>
	<xtags:if test="count(//configuration) > 1">
		<ul class="tab">
			<xtags:forEach select="//configuration" sort="@nameUC">
				<xtags:variable id="configuration" select="."/>
				<% if (configuration.equals(session.getAttribute("configurationName"))) { %>
					<li class="active">
						<% out.write(XmlUtils.encodeChars(configuration)); %>
					</li>
				<% } else { %>
					<li>
						<a
							href="showConfigurationStatus.do?configuration=<%=java.net.URLEncoder.encode(configuration)%>"
							alt="<% out.write(XmlUtils.encodeChars(configuration)); %>"
							text="<% out.write(XmlUtils.encodeChars(configuration)); %>"
							>
							<% out.write(XmlUtils.encodeChars(configuration)); %>
						</a>
					</li>
				<% } %>
			</xtags:forEach>
		</ul>
	</xtags:if>
	<div class="tabpanel">
		<xtags:parse>
			<% out.write(XmlUtils.replaceNonValidXmlCharacters(request.getAttribute("adapters").toString())); %>
		</xtags:parse>
	
		<xtags:forEach select="//registeredAdapters">
			<xtags:forEach select="exceptions">
				<contentTable width="100%">
					<tbody>
						<tr>
							<subHeader><h1 style="color:red">Exceptions</h1></subHeader>
						</tr>
						<xtags:forEach select="exception">
							<tr>
								<td><xtags:valueOf select="."/></td>
							</tr>
						</xtags:forEach>
					</tbody>
				</contentTable>
				<br/>
			</xtags:forEach>
			<xtags:forEach select="warnings">
				<contentTable width="100%">
					<tbody>
						<tr>
							<subHeader align="center"><h7>Warnings</h7></subHeader>
						</tr>
						<xtags:forEach select="warning">
							<tr>
								<td>
									<xtags:choose>
										<xtags:when test="@severe='true'">
											<font color="red">
												<xtags:valueOf select="."/>
											</font>
										</xtags:when>
										<xtags:otherwise>
											<xtags:valueOf select="."/>
										</xtags:otherwise>
									</xtags:choose>
								</td>
							</tr>
						</xtags:forEach>
					</tbody>
				</contentTable>
				<br/>
			</xtags:forEach>
		</xtags:forEach>
	
		<contentTable width="100%">
			<tbody>
				<tr>
					<subHeader colspan="10" align="center"><h7>Summary</h7></subHeader>
				</tr>
				<tr>
					<subHeader>State</subHeader>
					<subHeader align="center"><image type="started" title="started"/></subHeader>
					<subHeader align="center"><image type="starting" title="starting"/></subHeader>
					<subHeader align="center"><image type="stopped" title="stopped"/></subHeader>
					<subHeader align="center"><image type="stopping" title="stopping"/></subHeader>
					<subHeader align="center"><image type="error" title="error"/></subHeader>
					<subHeader align="center">INFO messages</subHeader>
					<subHeader align="center">WARN messages</subHeader>
					<subHeader align="center">ERROR messages</subHeader>
					<subHeader>Actions</subHeader>
				</tr>
				<% String configName = (String) session.getAttribute("configurationName");
				   String configFlowUrl = "rest/showFlowDiagram?configuration=" + FileUtils.encodeFileName(configName); %>
				<tr>
					<subHeader>Adapters</subHeader>
					<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/adapterState/@started"/></td>
					<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/adapterState/@starting"/></td>
					<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/adapterState/@stopped"/></td>
					<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/adapterState/@stopping"/></td>
					<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/adapterState/@error"/></td>
					<td rowspan="2" class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/messageLevel/@info"/></td>
					<td rowspan="2" class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/messageLevel/@warn"/></td>
					<td rowspan="2" class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/messageLevel/@error"/></td>
					<td rowspan="2" width="200">
						<imagelink
							href="adapterHandler.do"
							type="stop"
							alt="stop all adapters"
							>
							<parameter name="action">stopadapter</parameter>
							<parameter name="configurationName"><% out.write(java.net.URLEncoder.encode((String)session.getAttribute("configurationName"))); %></parameter>
							<parameter name="adapterName">*ALL*</parameter>
						</imagelink>
						<imagelink
							href="adapterHandler.do"
							type="start"
							alt="start all adapters"
							>
							<parameter name="action">startadapter</parameter>
							<parameter name="configurationName"><% out.write(java.net.URLEncoder.encode((String)session.getAttribute("configurationName"))); %></parameter>
							<parameter name="adapterName">*ALL*</parameter>
						</imagelink>
						<% if ("*ALL*".equals(session.getAttribute("configurationName"))) { %>
							<imagelink
								href="adapterHandlerAsAdmin.do"
								type="fullreload"
								alt="full reload"
								>
								<parameter name="action">fullreload</parameter>
							</imagelink>
						<% } else {%>
						<imagelink
							href="adapterHandler.do"
							type="reload"
							alt="reload configuration"
							>
							<parameter name="action">reload</parameter>
							<parameter name="configurationName"><% out.write(java.net.URLEncoder.encode((String)session.getAttribute("configurationName"))); %></parameter>
						</imagelink>
						<% } %>
						<imagelink
							href="<%=XmlUtils.encodeChars(configFlowUrl)%>"
							type="flow"
							alt="show adapter references"
							newwindow="true"
							/>
					</td>
				</tr>
				<tr>
					<subHeader>Receivers</subHeader>
					<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/receiverState/@started"/></td>
					<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/receiverState/@starting"/></td>
					<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/receiverState/@stopped"/></td>
					<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/receiverState/@stopping"/></td>
					<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/receiverState/@error"/></td>
				</tr>
				<xtags:forEach select="//registeredAdapters/configurationMessages">
					<tr>
						<subHeader colspan="9">Messages</subHeader>
						<subHeader>Flow</subHeader>
					</tr>
					<% boolean first = true; %>
					<% int nrOfConfigurationMessages = 0; %>
					<xtags:forEach select="configurationMessage">
						<% nrOfConfigurationMessages++; %>
					</xtags:forEach>
					<xtags:forEach select="configurationMessage">
						<tr>
							<td colspan="9" class="messagesRow">
								<xtags:choose>
									<xtags:when test="@level='ERROR'">
										<font color="red">
											<xtags:valueOf select="@date"/> : <xtags:valueOf select="."/>
										</font>
									</xtags:when>
									<xtags:when test="@level='WARN'">
										<font color="orange">
											<xtags:valueOf select="@date"/> : <xtags:valueOf select="."/>
										</font>
									</xtags:when>
									<xtags:otherwise>
										<xtags:valueOf select="@date"/> : <xtags:valueOf select="."/>
									</xtags:otherwise>
								</xtags:choose>
							</td>
							<% if (first) {
								boolean active = true;
								// https://code.google.com/p/chromium/issues/detail?id=423749
								if (request.getHeader("User-Agent").contains("Chrome")
										|| request.getHeader("User-Agent").contains("MSIE 8.0")
										|| request.getHeader("User-Agent").contains("MSIE 9.0")
										|| request.getHeader("User-Agent").contains("MSIE 10.")) {
									active = false;
								}
							
							%>
								<td rowspan="<%= nrOfConfigurationMessages %>" width="200"<% if (active) { %> bgcolor="white"<% } %>>
									<% if (active) { %>
										<a href="<%=XmlUtils.encodeChars(configFlowUrl)%>" newwindow="true">
											<img src="<%=XmlUtils.encodeChars(configFlowUrl)%>" title="IBIS adapter references" width="200" height="200"/>
										</a>
									<% } %>
								</td>
							<% } %>
						</tr>
						<% first = false; %>
					</xtags:forEach>
				</xtags:forEach>
			</tbody>
		</contentTable>
	
		<xtags:forEach select="//registeredAdapters">

			<xtags:choose>
				<xtags:when test="//registeredAdapters/@all='true'">
					<br/>
					<contentTable width="100%">
						<tbody>
							<xtags:variable id="alert" select="//registeredAdapters/@alert"/>
							<tr>
								<subHeader colspan="10" align="center"><h7>
									<xtags:choose>
										<xtags:when test="$alert='true'">Adapters (only alerts)</xtags:when>
										<xtags:otherwise>Adapters</xtags:otherwise>
									</xtags:choose>
								</h7></subHeader>
							</tr>
							<tr>
								<subHeader>Configuration</subHeader>
								<subHeader>Name</subHeader>
								<subHeader>State</subHeader>
								<subHeader>Up since</subHeader>
								<subHeader>Last message</subHeader>
								<subHeader>Messages with error</subHeader>
								<subHeader>Messages processed/in process</subHeader>
								<subHeader>State receiver(s)</subHeader>
								<subHeader>Log</subHeader>
							</tr>
							<xtags:forEach select="adapter" sort="concat(@configUC,'|',@nameUC)">
								<xtags:variable id="stateAlert" select="@stateAlert"/>
								<xtags:variable id="countMessagesAlert" select="number(adapterMessages/@warn)+number(adapterMessages/@error)"/>
								<% if (Boolean.valueOf(alert)==false || (Boolean.valueOf(alert)==true && (Boolean.valueOf(stateAlert)==true || Integer.valueOf(countMessagesAlert)>0))) { %>
									<xtags:variable id="adapterState" select="@state"/>
									<tr ref="adapterRow">
										<td>
											<xtags:valueOf select="@config"/>
										</td>
										<td>
											<xtags:valueOf select="@name"/>
										</td>
										<td>
											<%   if (RunStateEnum.STOPPED.isState(adapterState)) { %>
												<image type="stopped" title="stopped"/>
											<% } if (RunStateEnum.STARTED.isState(adapterState)) { %>
												<image type="started" title="started"/>
											<% } if (RunStateEnum.ERROR.isState(adapterState)) { %>
												<image type="error" title="error"/>
											<% } if (RunStateEnum.STOPPING.isState(adapterState)) { %>
												<image type="stopping" title="stopping"/>
											<% } if (RunStateEnum.STARTING.isState(adapterState)) { %>
												<image type="starting" title="starting"/>
											<%}%>
										</td>
										<td>
											<xtags:valueOf select="@upSince"/>
											<xtags:if test="@upSinceAge!=''"> (<xtags:valueOf select="@upSinceAge"/>)</xtags:if>
										</td>
										<td>
											<xtags:valueOf select="@lastMessageDate"/>
											<xtags:if test="@lastMessageDateAge!=''"> (<xtags:valueOf select="@lastMessageDateAge"/>)</xtags:if>
										</td>
										<td align="right"><xtags:valueOf select="@messagesInError"/></td>
										<td align="right"><xtags:valueOf select="@messagesProcessed"/>/<xtags:valueOf select="@messagesInProcess"/></td>
										<td>
											<xtags:forEach select="receivers/receiver">
												<xtags:variable id="receiverState" select="@state"/>
												<%   if (RunStateEnum.STOPPED.isState(receiverState) ){ %>
													<image type="stopped" title="stopped"/>
												<% } if (RunStateEnum.STARTED.isState(receiverState) ){ %>
													<image type="started" title="started"/>
												<% } if (RunStateEnum.ERROR.isState(receiverState) ){ %>
													<image type="error" title="error"/>
												<% } if (RunStateEnum.STOPPING.isState(receiverState) ){ %>
													<image type="stopping" title="stopping"/>
												<% } if (RunStateEnum.STARTING.isState(receiverState) ){ %>
													<image type="starting" title="starting"/>
												<%}%>
											</xtags:forEach>
										</td>
										<td>
											<xtags:choose>
												<xtags:when test="number(adapterMessages/@error)!=0">
														<image type="delete" title="error"/>
												</xtags:when>
												<xtags:when test="number(adapterMessages/@warn)!=0">
														<image type="error" title="warn"/>
												</xtags:when>
												<xtags:otherwise>
													<image type="check" title="info"/>
												</xtags:otherwise>
											</xtags:choose>
										</td>
									</tr>
								<%}%>
							</xtags:forEach>
						</tbody>
					</contentTable>
				</xtags:when>
				<xtags:otherwise>
					<xtags:forEach select="adapter" sort="@nameUC">
						<xtags:variable id="adapterName" select="@name"/>
						<% String adapterFlowUrl = "rest/showFlowDiagram/" + FileUtils.encodeFileName(adapterName); %>
						<br/>
						<contentTable width="100%">
							<tbody>
								<xtags:variable id="adapterState" select="@state"/>
								<tr><subHeader colspan="7"><h8><xtags:valueOf select="@name"/><xtags:if test="@description!=''"> (<xtags:valueOf select="@description"/>)</xtags:if></h8></subHeader></tr>
								<tr>
									<subHeader>State</subHeader>
									<subHeader>Configured</subHeader>
									<subHeader>Up since</subHeader>
									<subHeader>Last message</subHeader>
									<subHeader>Messages with error</subHeader>
									<subHeader>Messages processed/in process</subHeader>
									<subHeader>Actions</subHeader>
								</tr>
			
								<tr ref="adapterRow">
									<td>
										<%   if (RunStateEnum.STOPPED.isState(adapterState)) { %>
											<image type="stopped" title="stopped"/>
										<% } if (RunStateEnum.STARTED.isState(adapterState)) { %>
											<image type="started" title="started"/>
										<% } if (RunStateEnum.ERROR.isState(adapterState)) { %>
											<image type="error" title="error"/>
										<% } if (RunStateEnum.STOPPING.isState(adapterState)) { %>
											<image type="stopping" title="stopping"/>
										<% } if (RunStateEnum.STARTING.isState(adapterState)) { %>
											<image type="starting" title="starting"/>
										<%}%>
									</td>
									<td><booleanImage value="<xtags:valueOf select="@configured"/>"/></td>
									<td>
										<xtags:valueOf select="@upSince"/>
										<xtags:if test="@upSinceAge!=''"> (<xtags:valueOf select="@upSinceAge"/>)</xtags:if>
									</td>
									<td>
										<xtags:valueOf select="@lastMessageDate"/>
										<xtags:if test="@lastMessageDateAge!=''"> (<xtags:valueOf select="@lastMessageDateAge"/>)</xtags:if>
									</td>
									<td align="right"><xtags:valueOf select="@messagesInError"/></td>
									<td align="right"><xtags:valueOf select="@messagesProcessed"/>/<xtags:valueOf select="@messagesInProcess"/></td>
									<td>
										<xtags:variable id="adapterIsStarted" select="@started"/>
										<imagelinkMenu>
											<% if (RunStateEnum.STOPPED.isState(adapterState)){ %>
												<imagelink
													href="adapterHandler.do"
													type="start"
													alt="start">
													<parameter name="action">startadapter</parameter>
													<parameter name="adapterName"><%=java.net.URLEncoder.encode(adapterName)%></parameter>
												</imagelink>
											<% } if (RunStateEnum.STARTED.isState(adapterState) ||
													RunStateEnum.ERROR.isState(adapterState) ) { %>
												<imagelink
													href="adapterHandler.do"
													type="stop"
													alt="stop"
													>
													<parameter name="action">stopadapter</parameter>
													<parameter name="adapterName"><%=java.net.URLEncoder.encode(adapterName)%></parameter>
												 </imagelink>
											<% } %>
												<imagelink
													href="showAdapterStatistics.do"
													type="adapterStatistics"
													alt="show Adapter Statistics"
													>
													<parameter name="adapterName"><%=java.net.URLEncoder.encode(adapterName)%></parameter>
												 </imagelink>
												<imagelink
													href="<%=adapterFlowUrl %>"
													type="flow"
													alt="show Adapter Flow"
													newwindow="true"
													/>
											<xtags:valueOf select="@state"/>
										</imagelinkMenu>
									</td>
								</tr>
			
								<xtags:forEach select="receivers">
									<tr>
										<subHeader>State</subHeader>
										<subHeader colspan="2">Receiver name</subHeader>
										<xtags:choose>
											<xtags:when test="count(receiver[@pendingMessagesCount!=''])!=0">
												<subHeader>Listener/Sender</subHeader>
												<subHeader>Messages pending</subHeader>
											</xtags:when>
											<xtags:otherwise>
												<subHeader colspan="2">Listener/Sender</subHeader>
											</xtags:otherwise>							
										</xtags:choose>
										<subHeader>Messages received/retried/rejected</subHeader>
										<subHeader>Actions</subHeader>
									</tr>
			
									<xtags:forEach select="receiver">
										<xtags:variable id="receiverState" select="@state"/>
										<xtags:variable id="receiverName" select="@name"/>
										<xtags:variable id="hasInprocessStorage" select="@hasInprocessStorage"/>
										<xtags:variable id="hasErrorStorage" select="@hasErrorStorage"/>
										<xtags:variable id="hasMessageLog" select="@hasMessageLog"/>
										<xtags:variable id="isRestListener" select="@isRestListener"/>
										<xtags:variable id="restUriPattern" select="@restUriPattern"/>
										<xtags:variable id="isView" select="@isView"/>
										<xtags:variable id="isEsbJmsFFListener" select="@isEsbJmsFFListener"/>
										<tr>
											<td class="receiverRow">
										<%   if (RunStateEnum.STOPPED.isState(receiverState) ){ %>
											<image type="stopped" title="stopped"/>
										<% } if (RunStateEnum.STARTED.isState(receiverState) ){ %>
											<image type="started" title="started"/>
										<% } if (RunStateEnum.ERROR.isState(receiverState) ){ %>
											<image type="error" title="error"/>
										<% } if (RunStateEnum.STOPPING.isState(receiverState) ){ %>
											<image type="stopping" title="stopping"/>
										<% } if (RunStateEnum.STARTING.isState(receiverState) ){ %>
											<image type="starting" title="starting"/>
										<%}%>
											</td>
											<td colspan="2" class="receiverRow">
												<xtags:valueOf select="@name"/>
											</td>
											<xtags:choose>
												<xtags:when test="@pendingMessagesCount!=''">
													<td class="receiverRow">
														<%@ include file="receiverInfo.jsp" %>
												 	</td>
													<td class="receiverRow" align="right">
														<xtags:choose>
															<xtags:when test="@esbPendingMessagesCount!=''">
																<xtags:valueOf select="@pendingMessagesCount"/>/<xtags:valueOf select="@esbPendingMessagesCount"/>
															</xtags:when>
															<xtags:otherwise>
																<xtags:valueOf select="@pendingMessagesCount"/>
															</xtags:otherwise>							
														</xtags:choose>
												 	</td>
												</xtags:when>
												<xtags:otherwise>
													<td colspan="2" class="receiverRow">
														<%@ include file="receiverInfo.jsp" %>
												 	</td>
												</xtags:otherwise>							
											</xtags:choose>
											<td class="receiverRow" align="right"><xtags:valueOf select="@messagesReceived"/>/<xtags:valueOf select="@messagesRetried"/>/<xtags:valueOf select="@messagesRejected"/></td>
											<td nowrap="true" width="200">
												<% if (RunStateEnum.STOPPED.isState(receiverState)){ %>
													<imagelink
														href="adapterHandler.do"
														type="start"
														alt="start receiver">
														<parameter name="action">startreceiver</parameter>
														<parameter name="adapterName"><%=java.net.URLEncoder.encode(adapterName)%></parameter>
														<parameter name="receiverName"><%=java.net.URLEncoder.encode(receiverName)%></parameter>
													 </imagelink>
												<% } if ( RunStateEnum.STARTED.isState(receiverState) ||
														  RunStateEnum.ERROR.isState(receiverState) ) { %>
													<imagelink
														href="adapterHandler.do"
														type="stop"
														alt="stop receiver">
														<parameter name="action">stopreceiver</parameter>
														<parameter name="adapterName"><%=java.net.URLEncoder.encode(adapterName)%></parameter>
														<parameter name="receiverName"><%=java.net.URLEncoder.encode(receiverName)%></parameter>
													 </imagelink>
												<% } if ( "true".equalsIgnoreCase(isRestListener) && "true".equalsIgnoreCase(isView) ) { %>
													<imagelink
														href="<%=restUriPattern%>"
														type="showashtml"
														alt="<%=receiverName%>"/>
												<% } if ( "true".equalsIgnoreCase(isEsbJmsFFListener) && "true".equalsIgnoreCase(hasErrorStorage) ) { %>
													<imagelink
														href="adapterHandler.do"
														type="move"
														alt="move message">
														<parameter name="action">movemessage</parameter>
														<parameter name="adapterName"><%=java.net.URLEncoder.encode(adapterName)%></parameter>
														<parameter name="receiverName"><%=java.net.URLEncoder.encode(receiverName)%></parameter>
													</imagelink>
												<% } if ( "true".equalsIgnoreCase(hasErrorStorage) ) { %>
													<xtags:choose>
														<xtags:when test="@errorStorageCount=-1">
															<image type="browseErrorStore" alt="errorStore"/>
														</xtags:when>
														<xtags:otherwise>
															<imagelink
																href="browser.do"
																type="browseErrorStore"
																alt="show contents of errorQueue"
																>
																<parameter name="storageType">errorlog</parameter>
																<parameter name="action">show</parameter>
																<parameter name="adapterName"><%=java.net.URLEncoder.encode(adapterName)%></parameter>
																<parameter name="receiverName"><%=java.net.URLEncoder.encode(receiverName)%></parameter>
															 </imagelink> (<xtags:valueOf select="@errorStorageCount"/>)
														</xtags:otherwise>
													</xtags:choose>
												<% } if ( "true".equalsIgnoreCase(hasMessageLog) ) { %>
													<xtags:choose>
														<xtags:when test="@messageLogCount=-1">
															<image type="browseMessageLog" alt="messageLog"/>
														</xtags:when>
														<xtags:otherwise>
															<imagelink
																href="browser.do"
																type="browseMessageLog"
																alt="show contents of messageLog"
																>
																<parameter name="storageType">messagelog</parameter>
																<parameter name="action">show</parameter>
																<parameter name="adapterName"><%=java.net.URLEncoder.encode(adapterName)%></parameter>
																<parameter name="receiverName"><%=java.net.URLEncoder.encode(receiverName)%></parameter>
															 </imagelink> (<xtags:valueOf select="@messageLogCount"/>)
														</xtags:otherwise>
													</xtags:choose>
												<% } %>
											</td>
										</tr>
									</xtags:forEach>
								</xtags:forEach>
			
								<xtags:if test="count(pipes/pipe[@sender!='' and (@isJdbcSender='true'=false() or @hasMessageLog='true')])!=0">
									<tr>
										<subHeader colspan="3">Message sending pipes</subHeader>
										<subHeader colspan="3">Sender/Listener</subHeader>
										<subHeader>Show log</subHeader>
									</tr>
									<xtags:forEach select="pipes/pipe[@sender!='' and (@isJdbcSender='true'=false() or @hasMessageLog='true')]">
										<xtags:variable id="pipeName" select="@name"/>
										<xtags:variable id="hasMessageLog" select="@hasMessageLog"/>
										<xtags:variable id="messageLogCount" select="@messageLogCount"/>
										<tr>
											<td colspan="3" class="receiverRow"><xtags:valueOf select="@name"/></td>
											<td colspan="3" class="receiverRow">
												<xtags:valueOf select="@sender"/>
												<xtags:if test="@destination!=''">(<xtags:valueOf select="@destination"/>)</xtags:if>
												<xtags:if test="@listenerClass!=''">/<xtags:valueOf select="@listenerClass"/>
													<xtags:if test="@listenerDestination!=''">(<xtags:valueOf select="@listenerDestination"/>)</xtags:if>
												</xtags:if>
											</td>
											<td>
											<%  if ( "true".equalsIgnoreCase(hasMessageLog) ) { %>
												<xtags:choose>
													<xtags:when test="@messageLogCount=-1">
														<image type="browseMessageLog" alt="messageLog"/>
													</xtags:when>
													<xtags:otherwise>
														<imagelink
															href="browser.do"
															type="browseMessageLog"
															alt="show contents of messageLog"
															>
															<parameter name="storageType">messagelog</parameter>
															<parameter name="action">show</parameter>
															<parameter name="adapterName"><%=java.net.URLEncoder.encode(adapterName)%></parameter>
															<parameter name="pipeName"><%=java.net.URLEncoder.encode(pipeName)%></parameter>
														 </imagelink> (<xtags:valueOf select="@messageLogCount"/>)
													</xtags:otherwise>
												</xtags:choose>
											<% } else { %>&#160;<% } %>
											</td>
										</tr>
									</xtags:forEach>
								</xtags:if>
			
								<xtags:forEach select="adapterMessages">
									<tr>
										<subHeader colspan="6">Messages</subHeader>
										<subHeader>Flow</subHeader>
									</tr>
									<% boolean first = true; %>
									<% int nrOfAdapterMessages = 0; %>
									<xtags:forEach select="adapterMessage">
										<% nrOfAdapterMessages++; %>
									</xtags:forEach>
									<xtags:forEach select="adapterMessage">
										<tr>
											<td colspan="6" class="messagesRow">
												<xtags:choose>
													<xtags:when test="@level='ERROR'">
														<font color="red">
															<xtags:valueOf select="@date"/> : <xtags:valueOf select="."/>
														</font>
													</xtags:when>
													<xtags:when test="@level='WARN'">
														<font color="orange">
															<xtags:valueOf select="@date"/> : <xtags:valueOf select="."/>
														</font>
													</xtags:when>
													<xtags:otherwise>
														<xtags:valueOf select="@date"/> : <xtags:valueOf select="."/>
													</xtags:otherwise>
												</xtags:choose>
											</td>
											<% if (first) {
												boolean active = true;
												// https://code.google.com/p/chromium/issues/detail?id=423749
												if (request.getHeader("User-Agent").contains("Chrome")
														|| request.getHeader("User-Agent").contains("MSIE 8.0")
														|| request.getHeader("User-Agent").contains("MSIE 9.0")
														|| request.getHeader("User-Agent").contains("MSIE 10.")) {
													active = false;
												}
											
											%>
												<td rowspan="<%= nrOfAdapterMessages %>" width="200"<% if (active) { %> bgcolor="white"<% } %>>
													<% if (active) { %>
														<a href="<%=adapterFlowUrl %>" newwindow="true">
															<img src="<%=adapterFlowUrl %>" title="<%=adapterName%>" width="200" height="200"/>
														</a>
													<% } %>
												</td>
											<% } %>
										</tr>
										<% first = false; %>
									</xtags:forEach>
								</xtags:forEach>
			
							</tbody>
						</contentTable>
					</xtags:forEach> <!-- adapter -->
				</xtags:otherwise>
			</xtags:choose>
			
		</xtags:forEach>
	</div>
</page>
