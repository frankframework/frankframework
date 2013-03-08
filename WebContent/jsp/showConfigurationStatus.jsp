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
				<td></td>
				<subHeader colspan="5" align="center">count</subHeader>
			</tr>
			<tr>
				<subHeader>state</subHeader>
				<subHeader><img src="images/connected.gif" title="started"/></subHeader>
				<subHeader><img src="images/connecting.gif" title="starting"/></subHeader>
				<subHeader><img src="images/disconnected.gif" title="stopped"/></subHeader>
				<subHeader><img src="images/disconnecting.gif" title="stopping"/></subHeader>
				<subHeader><img src="images/error.gif" title="error"/></subHeader>
			</tr>
			<tr>
				<subHeader>adapters</subHeader>
				<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/adapterState/@started"/></td>
				<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/adapterState/@starting"/></td>
				<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/adapterState/@stopped"/></td>
				<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/adapterState/@stopping"/></td>
				<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/adapterState/@error"/></td>
			</tr>
			<tr>
				<subHeader>receivers</subHeader>
				<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/receiverState/@started"/></td>
				<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/receiverState/@starting"/></td>
				<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/receiverState/@stopped"/></td>
				<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/receiverState/@stopping"/></td>
				<td class="receiverRow" align="right"><xtags:valueOf select="//registeredAdapters/summary/receiverState/@error"/></td>
			</tr>
		</tbody>
	</contentTable>
	<br/><br/><br/>

	<imagelink
			href="adapterHandler.do"
			type="stop"
			alt="stopadapter"
			text="Stop all adapters"
			>
			<parameter name="action">stopadapter</parameter>
			<parameter name="adapterName">**ALL**</parameter>
	 </imagelink>
	<imagelink
			href="adapterHandler.do"
			type="start"
			alt="start"
			text="Start all adapters"
			>
			<parameter name="action">startadapter</parameter>
			<parameter name="adapterName">**ALL**</parameter>
	 </imagelink>
	<imagelink
			href="images/flow/IBIS.svg"
			type="flow"
			alt="flow"
			text="Show adapter references"
			newwindow="true"
			/>


	<xtags:forEach select="//registeredAdapters">

		<xtags:forEach select="exceptions">
			<imagelink
					href="ConfigurationServlet"
					type="reload"
					alt="reload"
					text="Reload Configuration"
					>
			 </imagelink>
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
						<subHeader>Messages with error</subHeader>
						<subHeader>Messages processed</subHeader>
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
						<td><xtags:valueOf select="@upSince"/></td>
						<td align="right"><xtags:valueOf select="@messagesInError"/></td>
						<td align="right"><xtags:valueOf select="@messagesProcessed"/></td>
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
									alt="flow"
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
							<subHeader>listener/sender</subHeader>

							<subHeader>messages received/retried/rejected</subHeader>
							<subHeader>Actions</subHeader>
						</tr>

						<xtags:forEach select="receiver">
							<xtags:variable id="receiverState" select="@state"/>
							<xtags:variable id="receiverName" select="@name"/>
							<xtags:variable id="hasInprocessStorage" select="@hasInprocessStorage"/>
							<xtags:variable id="hasErrorStorage" select="@hasErrorStorage"/>
							<xtags:variable id="hasMessageLog" select="@hasMessageLog"/>
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

							 	<td class="receiverRow">
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
							<% } if ( "true".equalsIgnoreCase(hasErrorStorage) ) { %>
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
							<% } if ( "true".equalsIgnoreCase(hasMessageLog) ) { %>
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
								<% } %>
								</td>
							</tr>
						</xtags:forEach>
					</xtags:forEach>

					<xtags:if test="count(pipes/pipe[@sender!='' and (@isJdbcSender='true'=false() or @hasMessageLog='true')])!=0">
						<tr>
							<td></td>
							<subHeader colspan="3">message sending pipes</subHeader>
							<subHeader colspan="2">sender/listener</subHeader>
							<subHeader>Show Log</subHeader>
						</tr>
						<xtags:forEach select="pipes/pipe[@sender!='' and (@isJdbcSender='true'=false() or @hasMessageLog='true')]">
							<xtags:variable id="pipeName" select="@name"/>
							<xtags:variable id="hasMessageLog" select="@hasMessageLog"/>
							 <tr >
								<td></td>
								<td colspan="3" class="receiverRow"><xtags:valueOf select="@name"/></td>
								<td colspan="2" class="receiverRow">
									<xtags:valueOf select="@sender"/>
							 		<xtags:if test="@destination!=''">(<xtags:valueOf select="@destination"/>)</xtags:if>
							 		<xtags:if test="@listenerClass!=''">/<xtags:valueOf select="@listenerClass"/>
								 		<xtags:if test="@listenerDestination!=''">(<xtags:valueOf select="@listenerDestination"/>)</xtags:if>
							 		</xtags:if>
								</td>
								<td>
								<%  if ( "true".equalsIgnoreCase(hasMessageLog) ) { %>
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
								<% } else { %>&#160;<% } %>
								</td>
							</tr>
						</xtags:forEach>
					</xtags:if>



					<xtags:forEach select="adapterMessages">
						<tr>
							<td></td>
							<subHeader colspan="6">Messages</subHeader>
						</tr>
						<xtags:forEach select="adapterMessage">
						<tr>
							<td></td>
							<td colspan="6" class="messagesRow">
								<xtags:valueOf select="@date"/> : <xtags:valueOf select="."/>
							</td>
						</tr>
						</xtags:forEach>
					</xtags:forEach>


				</xtags:forEach> <!-- adapter -->
				</tbody>
			</contentTable>
	</xtags:forEach>



</page>

