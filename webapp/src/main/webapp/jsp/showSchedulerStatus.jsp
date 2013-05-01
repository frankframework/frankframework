<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>


<page title="Show Scheduler Status">
		<xtags:parse>
			<%=request.getAttribute("metadata")%>
		</xtags:parse>
		
		<xtags:forEach select="//schedulerMetaData">
			<contentTable>
				<caption>Scheduler</caption>
				<tbody>
				<tr><th>Parameter</th>
					<th>Value</th>
					<th>Action</th>
				</tr>
					<tr><td>Name</td><td><xtags:valueOf select="@schedulerName"/></td></tr>
					<tr><td>schedulerInstanceId</td><td><xtags:valueOf select="@schedulerInstanceId"/></td></tr>
					<tr><td>isStarted</td><td><booleanImage value="<xtags:valueOf select="@isStarted"/>"/></td><td>
						<xtags:if test="@isStarted='False'">
								<imagelink 
									href="schedulerHandler.do"
									type="start"
									alt="start scheduler">
									<parameter name="action">startScheduler</parameter>
								 </imagelink>
						</xtags:if>
						</td>
					</tr>
					<tr><td>isPaused</td><td><booleanImage value="<xtags:valueOf select="@isPaused"/>"/></td><td>
						<xtags:if test="@isPaused='False'">
								<imagelink 
									href="schedulerHandler.do"
									type="pause"
									alt="pause scheduler">
									<parameter name="action">pauseScheduler</parameter>
								 </imagelink>
						</xtags:if>
						<xtags:if test="@isPaused='True'">
								<imagelink 
									href="schedulerHandler.do"
									type="start"
									alt="start scheduler">
									<parameter name="action">startScheduler</parameter>
								 </imagelink>
						</xtags:if>
						</td>
					</tr>
					<tr><td>runningSince</td><td><xtags:valueOf select="@runningSince"/></td></tr>
					<tr><td>numJobsExecuted</td><td><xtags:valueOf select="@numJobsExecuted"/></td></tr>
					<tr><td>isSchedulerRemote</td><td><booleanImage value="<xtags:valueOf select="@isSchedulerRemote"/>"/></td></tr>
					<tr><td>threadPoolSize</td><td><xtags:valueOf select="@threadPoolSize"/></td></tr> 
					<tr><td>schedulerClass</td><td><xtags:valueOf select="@schedulerClass"/></td></tr> 
					<tr><td>version</td><td><xtags:valueOf select="@version"/></td></tr>
					<tr><td>jobStoreClass</td><td><xtags:valueOf select="@jobStoreClass"/></td></tr> 
					<tr><td>jobStoreSupportsPersistence</td><td><booleanImage value="<xtags:valueOf select="@jobStoreSupportsPersistence"/>"/></td></tr>
				</tbody>
			</contentTable>
	</xtags:forEach>	

	<br/>
	<br/>


	<xtags:parse>
			<%=request.getAttribute("jobdata")%>
	</xtags:parse>


	<xtags:forEach select="//jobGroups/jobGroup">
		<xtags:variable id="jobGroup" select="@name"/>


		<xtags:forEach select="jobs">
			<contentTable>
				<caption>Jobs in jobgroup <xtags:valueOf select="../@name"/></caption>
				<tbody>

				<xtags:forEach select="job/jobDetail">
					<tr><th>Name</th>
						<th>description</th>
						<th>jobClass</th>
						<th>Action</th>
					</tr>
					<tr><td><xtags:valueOf select="@jobName"/></td>
						<td><xtags:valueOf select="@description"/></td>
						<td><xtags:valueOf select="@jobClass"/></td>
						<td>		
							<xtags:variable id="jobName" select="@jobName"/>
							<xtags:variable id="groupName" select="@groupName"/>
							
							<imagelink 
								href="schedulerHandler.do"
								type="delete"
								alt="delete job">
								<parameter name="action">deleteJob</parameter>
								<parameter name="jobName"><%=java.net.URLEncoder.encode(jobName)%></parameter>
								<parameter name="groupName"><%=java.net.URLEncoder.encode(groupName)%></parameter>
							 </imagelink>
							<imagelink 
								href="schedulerHandler.do"
								type="start"
								alt="start job">
								<parameter name="action">triggerJob</parameter>
								<parameter name="jobName"><%=java.net.URLEncoder.encode(jobName)%></parameter>
								<parameter name="groupName"><%=java.net.URLEncoder.encode(groupName)%></parameter>
							 </imagelink>
							
						</td>
					</tr>
						<xtags:forEach select="../triggersForJob/triggerDetail">
						<tr>
							<td></td>
							<td colspan="3">

								<table>
								<tr>
									<td>Group</td><td><xtags:valueOf select="@triggerGroup"/></td>
								</tr>
								<tr>
									<td>StartTime</td><td><xtags:valueOf select="@startTime"/></td>
								</tr>
								<tr>
									<td>next fire</td><td><xtags:valueOf select="@nextFireTime"/></td>
									<td>cronExpression</td><td><xtags:valueOf select="@cronExpression"/></td>
								</tr>
								</table>
							</td>
							<td>
							 </td>
						</tr>
						</xtags:forEach> <!-- trigger -->

				
					<xtags:forEach select="../jobMessages">
						<tr>
							<td></td>
							<subHeader colspan="3">Messages</subHeader>
						</tr>
						<xtags:forEach select="jobMessage">
						<tr>
							<td></td>
							<td colspan="3" class="messagesRow">
								<xtags:valueOf select="@date"/> : <xtags:valueOf select="."/>
							</td>
						</tr>
						</xtags:forEach>
					</xtags:forEach>
				</xtags:forEach> <!-- jobDetail -->
				</tbody>
			</contentTable>
		</xtags:forEach> <!-- job -->



	</xtags:forEach> <!-- jobGroups/jobGroup -->



</page>		


  

