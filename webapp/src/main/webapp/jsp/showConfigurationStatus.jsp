<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>
<%@ page import="nl.nn.adapterframework.util.RunStateEnum" %>
<%@ page import="nl.nn.adapterframework.util.FileUtils" %> 


<page title="Show configuration status" refresh="showConfigurationStatus.do">

	<xtags:parse>
			<bean:write name="adapters" scope="request" filter="false"/>
	</xtags:parse>

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
						<parameter name="adapterName">**ALL**</parameter>
					 </imagelink>
					<imagelink
						href="adapterHandler.do"
						type="start"
						alt="start all adapters"
						>
						<parameter name="action">startadapter</parameter>
						<parameter name="adapterName">**ALL**</parameter>
					 </imagelink>
					<imagelink
						href="ConfigurationServlet"
						type="reload"
						alt="reload configuration"
						/>
					<imagelink
						href="images/flow/IBIS.svg"
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
		</tbody>
	</contentTable>

	<xtags:forEach select="//registeredAdapters">

		<xtags:forEach select="exceptions">
			<br/>
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
		</xtags:forEach>

		<xtags:forEach select="warnings">
			<br/>
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
		</xtags:forEach>

		<xtags:forEach select="adapter" sort="@nameUC">
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
							<xtags:variable id="adapterName" select="@name"/>
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
										href="images/flow/<%=FileUtils.encodeFileName(adapterName)%>.svg"
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
											<xtags:valueOf select="@pendingMessagesCount"/>
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
									if (request.getHeader("User-Agent").contains("MSIE 8.0")
											|| request.getHeader("User-Agent").contains("Chrome")) {
										active = false;
									}
								
								%>
									<td rowspan="<%= nrOfAdapterMessages %>" width="200"<% if (active) { %> bgcolor="white"<% } %>>
										<% if (active) { %>
											<a href="images/flow/<%=FileUtils.encodeFileName(adapterName)%>.svg" newwindow="true">
												<img src="images/flow/<%=java.net.URLEncoder.encode(adapterName)%>.svg" title="<%=adapterName%>" width="200" height="200"/>
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

	</xtags:forEach>

	<% if (!request.getHeader("User-Agent").contains("MSIE 8.0")) { %>
		<br/>
		<contentTable width="100%">
			<tbody>
				<tr>
					<subHeader colspan="12" align="center"><h7>Adapter references</h7></subHeader>
				</tr>
				<tr>
					<td colspan="12">
						<a href="images/flow/IBIS.svg" newwindow="true">
							<img src="images/flow/IBIS.svg" title="adapter references" width="100%"/>
						</a>
					</td>
				</tr>
			</tbody>
		</contentTable>
	<% } %>
	
</page>
