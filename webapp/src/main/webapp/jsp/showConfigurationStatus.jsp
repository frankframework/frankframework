<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>
<%@ page import="nl.nn.adapterframework.util.RunStateEnum" %>
<%@ page import="nl.nn.adapterframework.util.FileUtils" %> 




<page title="Show configurationStatus" refresh="showConfigurationStatus.do">

	<xtags:parse>
			<bean:write name="adapters" scope="request" filter="false"/>
	</xtags:parse>

	<contentTable>
		<caption>Summary</caption>
		<tbody>
			<tr>
				<subHeader>State</subHeader>
				<subHeader align="center"><img src="images/connected.gif" title="started"/></subHeader>
				<subHeader align="center"><img src="images/connecting.gif" title="starting"/></subHeader>
				<subHeader align="center"><img src="images/disconnected.gif" title="stopped"/></subHeader>
				<subHeader align="center"><img src="images/disconnecting.gif" title="stopping"/></subHeader>
				<subHeader align="center"><img src="images/error.gif" title="error"/></subHeader>
				<subHeader align="center">Actions</subHeader>
				<td></td>
				<subHeader>Level</subHeader>
				<subHeader align="center">INFO</subHeader>
				<subHeader align="center">WARN</subHeader>
				<subHeader align="center">ERROR</subHeader>
			</tr>
			<tr>
				<subHeader>Adapters</subHeader>
				<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/adapterState/@started"/></td>
				<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/adapterState/@starting"/></td>
				<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/adapterState/@stopped"/></td>
				<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/adapterState/@stopping"/></td>
				<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/adapterState/@error"/></td>
				<td>
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
				<td></td>
				<subHeader>Messages</subHeader>
				<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/messageLevel/@info"/></td>
				<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/messageLevel/@warn"/></td>
				<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/messageLevel/@error"/></td>
			</tr>
			<tr>
				<subHeader>Receivers</subHeader>
				<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/receiverState/@started"/></td>
				<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/receiverState/@starting"/></td>
				<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/receiverState/@stopped"/></td>
				<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/receiverState/@stopping"/></td>
				<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/receiverState/@error"/></td>
				<td></td>
			</tr>
		</tbody>
	</contentTable>
	<br/>

	<xtags:forEach select="//registeredAdapters">
		<xtags:forEach select="exceptions">
			<contentTable>
				<caption>Exceptions</caption>
				<tbody>
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
			<contentTable>
				<caption>Warnings</caption>
				<tbody>
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

			<contentTable>
				<caption>Configured Adapters</caption>
				<tbody>
				<xtags:forEach select="adapter" sort="@nameUC">
					<xtags:variable id="adapterState" select="@state"/>
					<tr>
						<subHeader>Adapter<img src="images/pixel.gif" widsubHeader="150px" height="1px"/></subHeader>
						<subHeader>State</subHeader>
						<subHeader>Configured</subHeader>
						<subHeader>up since</subHeader>
						<subHeader>Last Message</subHeader>
						<subHeader>Messages with error</subHeader>
						<subHeader>Messages processed/in process</subHeader>
						<subHeader>Actions</subHeader>
					</tr>


					<tr ref="adapterRow">
						<td><xtags:valueOf select="@name"/></td>
						<td>
							<%    if (RunStateEnum.STOPPED.isState(adapterState)) { %>
								<img src="images/disconnected.gif" title="stopped"/>
							<% } if (RunStateEnum.STARTED.isState(adapterState)) { %>
								<img src="images/connected.gif" title="started"/>
							<% } if (RunStateEnum.ERROR.isState(adapterState)) { %>
								<img src="images/error.gif" title="error"/>
							<% } if (RunStateEnum.STOPPING.isState(adapterState)) { %>
								<img src="images/disconnecting.gif" title="stopping"/>
							<% } if (RunStateEnum.STARTING.isState(adapterState)) { %>
								<img src="images/connecting.gif" title="starting"/>
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

							<% } if ( RunStateEnum.STARTED.isState(adapterState) ||
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
							<td></td>
							<subHeader>state</subHeader>
							<subHeader colspan="2">receiver name</subHeader>
							<subHeader colspan="2">listener/sender</subHeader>

							<subHeader>messages received/retried/rejected</subHeader>
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
							<xtags:variable id="restMethod" select="@restMethod"/>
							<xtags:variable id="isEsbJmsFFListener" select="@isEsbJmsFFListener"/>
							 <tr >
								<td></td>
								<td class="receiverRow">
							<%    if (RunStateEnum.STOPPED.isState(receiverState) ){ %>
								<img src="images/disconnected.gif" title="stopped"/>
							<% } if (RunStateEnum.STARTED.isState(receiverState) ){ %>
								<img src="images/connected.gif" title="started"/>
							<% } if (RunStateEnum.ERROR.isState(receiverState) ){ %>
								<img src="images/error.gif" title="error"/>
							<% } if (RunStateEnum.STOPPING.isState(receiverState) ){ %>
								<img src="images/disconnecting.gif" title="stopping"/>
							<% } if (RunStateEnum.STARTING.isState(receiverState) ){ %>
								<img src="images/connecting.gif" title="starting"/>
							<%}%>

								</td>
								<td colspan="2" class="receiverRow">
									<xtags:valueOf select="@name"/>
								</td>

							 	<td colspan="2" class="receiverRow">
							 		<xtags:valueOf select="@listenerClass"/>
							 		<xtags:if test="@listenerDestination!=''">(<xtags:valueOf select="@listenerDestination"/>)</xtags:if>
							 		<xtags:if test="@senderClass!=''">/<xtags:valueOf select="@senderClass"/>
								 		<xtags:if test="@senderDestination!=''">(<xtags:valueOf select="@senderDestination"/>)</xtags:if>
							 		</xtags:if>
									<xtags:if test="@threadCount!=0">
									  <br/>( <xtags:valueOf select="@threadCount"/>/<xtags:valueOf select="@maxThreadCount"/>
									  thread<xtags:if test="@maxThreadCount!=1">s</xtags:if><xtags:if test="@threadCountControllable='true'">,
									  
										<imagelink
												href="adapterHandler.do"
												type="incthreads"
												alt="increase the maximum number of threads"
												text="inc"
											>
											<parameter name="action">incthreads</parameter>
											<parameter name="adapterName"><%=java.net.URLEncoder.encode(adapterName)%></parameter>
											<parameter name="receiverName"><%=java.net.URLEncoder.encode(receiverName)%></parameter>
										 </imagelink> -
										<imagelink
												href="adapterHandler.do"
												type="decthreads"
												alt="decrease the maximum number of threads"
												text="dec"
											>
											<parameter name="action">decthreads</parameter>
											<parameter name="adapterName"><%=java.net.URLEncoder.encode(adapterName)%></parameter>
											<parameter name="receiverName"><%=java.net.URLEncoder.encode(receiverName)%></parameter>
										 </imagelink>
										</xtags:if>
									  )
									</xtags:if>
							 	</td>
								<td class="receiverRow" align="right"><xtags:valueOf select="@messagesReceived"/>/<xtags:valueOf select="@messagesRetried"/>/<xtags:valueOf select="@messagesRejected"/></td>
								<td nowrap="true">
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
							<% } if ( "true".equalsIgnoreCase(isRestListener) && "GET".equalsIgnoreCase(restMethod) ) { %>
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
							<td></td>
							<subHeader colspan="3">message sending pipes</subHeader>
							<subHeader colspan="3">sender/listener</subHeader>
							<subHeader>Show Log</subHeader>
						</tr>
						<xtags:forEach select="pipes/pipe[@sender!='' and (@isJdbcSender='true'=false() or @hasMessageLog='true')]">
							<xtags:variable id="pipeName" select="@name"/>
							<xtags:variable id="hasMessageLog" select="@hasMessageLog"/>
							<xtags:variable id="messageLogCount" select="@messageLogCount"/>
							 <tr >
								<td></td>
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
							<td></td>
							<subHeader colspan="7">Messages</subHeader>
						</tr>
						<xtags:forEach select="adapterMessage">
						<tr>
							<td></td>
							<td colspan="7" class="messagesRow">
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
						</tr>
						</xtags:forEach>
					</xtags:forEach>


				</xtags:forEach> <!-- adapter -->
				</tbody>
			</contentTable>
	</xtags:forEach>
</page>

