
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>

  
<page title="Show Adapter Statistics" refresh="showAdapterStatistics.do">
  
	<xtags:parse>
		<bean:write name="adapterStatistics" scope="request" filter="false"/>
	</xtags:parse>
   
  	<xtags:stylesheet >
	  	<xtags:template match="/">
		 	<xtags:forEach select="//adapterStatistics">
				<br/>
				<contentTable>
					<caption>Adapter Statistics</caption>
					<tbody>
					<tr><th>Name</th>
						<th>up since</th>
						<th>Last Message</th>
						<th>Messages processed</th>
						<th>Messages in error</th>
						<th>Messages in process</th>
					</tr>
					<tr>
							<td><xtags:valueOf select="@name"/></td>
							<td><xtags:valueOf select="@upSince"/></td>
							<td><xtags:valueOf select="@lastMessageDate"/></td>
							<td align="right"><xtags:valueOf select="@messagesProcessed"/></td>
							<td align="right"><xtags:valueOf select="@messagesInError"/></td>
							<td align="right"><xtags:valueOf select="@messagesInProcess"/></td>
						
					</tr>
					</tbody>
				</contentTable>
				<br/><br/><br/>
				<contentTable>
					<caption>Total message processing duration<img src="images/pixel.gif" width="300px" height="1px"/></caption>
					<tbody>
							<xtags:forEach select="messageProcessingDuration/summary/item">
								<tr alternatingRows="true">
									<td colspan="2"><xtags:valueOf select="@name"/></td>
									<td align="right" colspan="2"><xtags:valueOf select="@value"/></td>
								</tr>
							</xtags:forEach>
					</tbody>
				</contentTable>

				<br/><br/><br/>
				<contentTable>
					<caption>Adapter statistics by the hour</caption>
					<tbody>
						<tr>
							<th>start time</th>
							<xtags:forEach select="messagesStartProcessingByHour/item">
								<td><xtags:valueOf select="@startTime"/></td>
							</xtags:forEach>
						</tr>
						<tr>
							<th>count</th>
							<xtags:forEach select="messagesStartProcessingByHour/item">
								<td align="right"><xtags:valueOf select="@count"/></td>
							</xtags:forEach>
						</tr>
					</tbody>
				</contentTable>
	
				<br/><br/><br/>
				<contentTable>
					<caption>counts for receivers</caption>
					<tbody>
					<tr>
						<th>name</th>
						<th>messages received/retried</th>
	
					</tr>
				<xtags:forEach select="receivers/receiver">
					<tr>
						<td><xtags:valueOf select="@name"/></td>
						<td><xtags:valueOf select="@messagesReceived"/>/<xtags:valueOf select="@messagesRetried"/></td>
					</tr>
				</xtags:forEach><!-- receiver -->
	
					</tbody>
				</contentTable>
	
				<xtags:forEach select="receivers/receiver/procStats">
				<contentTable>
					<caption>process statistics for receivers</caption>
					<tbody>
					<tr>
						<th>receiver</th>
						<th>threads processing</th>
						<xtags:forEach select="stat[1]/summary/item">
							<th><xtags:valueOf select="@name"/></th>
						</xtags:forEach>
					</tr>
					<xtags:forEach select="stat">
						<tr alternatingRows="true">
							<td><xtags:valueOf select="../../@name"/></td>
							<td><xtags:valueOf select="@name"/></td>
							<xtags:forEach select="summary/item">
								<td align="right"><xtags:valueOf select="@value"/></td>
							</xtags:forEach>
						</tr>
					</xtags:forEach> <!-- pipeStats -->
					</tbody>
				</contentTable>
				</xtags:forEach><!-- procStats -->
				<br/><br/>
				<xtags:forEach select="receivers/receiver/idleStats">
				<contentTable>
					<caption>idle statistics for receivers</caption>
					<tbody>
					<tr>
						<th>receiver</th>
						<th>threads processing</th>
						<xtags:forEach select="stat[1]/summary/item">
							<th><xtags:valueOf select="@name"/></th>
						</xtags:forEach>
					</tr>
					<xtags:forEach select="stat">
						
						<tr alternatingRows="true">
							<td><xtags:valueOf select="../../@name"/></td>
							<td><xtags:valueOf select="@name"/></td>
							<xtags:forEach select="summary/item">
								<td align="right"><xtags:valueOf select="@value"/></td>
							</xtags:forEach>
						</tr>
					</xtags:forEach> <!-- pipeStats -->
					</tbody>
				</contentTable>
				<p>idle between messages</p>
				</xtags:forEach><!-- procStats -->
	
				<br/><br/><br/>
				<xtags:forEach select="pipeline/pipeStats">
				<contentTable>
					<caption>Duration statistics per Pipe</caption>
					<tbody>
					<tr>
						<th>name</th>
						<xtags:forEach select="stat[1]/summary/item">
							<th><xtags:valueOf select="@name"/></th>
						</xtags:forEach>
					</tr>
	
					
					<xtags:forEach select="stat">
						<tr alternatingRows="true">
							<td><xtags:valueOf select="@name"/></td>
							<xtags:forEach select="summary/item">
								<td align="right"><xtags:valueOf select="@value"/></td>
							</xtags:forEach>
						</tr>
					</xtags:forEach> <!-- pipeStats -->
					</tbody>
				</contentTable>
				</xtags:forEach><!-- pipeline -->
		
				<br/><br/>
				<!-- display waiting statistics -->
				
				<xtags:forEach select="pipeline/waitStats">
				<contentTable>
					<caption>Waiting statistics per Pipe</caption>
					<tbody>
					<tr>
						<th>name</th>
						<xtags:forEach select="stat[1]/summary/item">
							<th><xtags:valueOf select="@name"/></th>
						</xtags:forEach>
					</tr>
				
					<xtags:forEach select="stat">				
						<tr alternatingRows="true">
							<td><xtags:valueOf select="@name"/></td>
							<xtags:forEach select="summary/item">
								<td align="right"><xtags:valueOf select="@value"/></td>
							</xtags:forEach>
						</tr>
					</xtags:forEach> <!-- pipeStats -->
					</tbody>
				</contentTable>
				<p>waiting time for availability of a pipe</p>
				</xtags:forEach><!-- pipeline -->
			<br/>
			<br/>
			<p>Duration statistics are in milliseconds</p>
					
				<xtags:forEach select="pipeline/sizeStats">
				<contentTable>
					<caption>Size statistics per Pipe</caption>
					<tbody>
					<tr>
						<th>name</th>
						<xtags:forEach select="stat[1]/summary/item">
							<th><xtags:valueOf select="@name"/></th>
						</xtags:forEach>
					</tr>
				
					<xtags:forEach select="stat">				
						<tr alternatingRows="true">
							<td><xtags:valueOf select="@name"/></td>
							<xtags:forEach select="summary/item">
								<td align="right"><xtags:valueOf select="@value"/></td>
							</xtags:forEach>
						</tr>
					</xtags:forEach> <!-- pipeStats -->
					</tbody>
				</contentTable>
				</xtags:forEach><!-- pipeline -->
			<br/>
			<br/>
			<p>Size statistics are in bytes</p>
					
			</xtags:forEach>
	  	</xtags:template>
		<xtags:template match="stat">
			<tr alternatingRows="true">
				<td>[<xtags:valueOf select="@name"/>]</td>
				<xtags:forEach select="summary/item">
					<td align="right">[<xtags:valueOf select="@value"/>]</td>
				</xtags:forEach>
			</tr>
		</xtags:template>

 	</xtags:stylesheet>
		
</page>		
