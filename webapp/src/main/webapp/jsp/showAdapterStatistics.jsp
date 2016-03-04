<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>

<page title="Show Adapter Statistics" refresh="showAdapterStatistics.do">

	<xtags:parse>
		<bean:write name="adapterStatistics" scope="request" filter="false"/>
	</xtags:parse>

	<xtags:forEach select="adapterStatistics">

		<br/>
		<contentTable>
			<caption>Adapter statistics</caption>
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
			<caption>Total message processing duration (in ms)</caption>
			<tbody>
					<xtags:forEach select="messageProcessingDuration/summary/item">
						<tr alternatingRows="true">
							<td><xtags:valueOf select="@name"/></td>
							<td align="right"><xtags:valueOf select="@value"/></td>
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
			<caption>Counts for receivers</caption>
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

		<br/><br/><br/>
		<contentTable>
			<caption>Process statistics for receivers (in ms)</caption>
			<tbody>
				<tr>
					<th>receiver</th>
					<th>threads processing</th>
					<xtags:forEach select="(receivers/receiver/procStats/stat)[1]/summary/item">
						<th><xtags:valueOf select="@name"/></th>
					</xtags:forEach>
				</tr>
				<xtags:forEach select="receivers/receiver/procStats/stat">
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

		<br/><br/><br/>
		<contentTable>
			<caption>Idle statistics for receivers (between messages)</caption>
			<tbody>
				<tr>
					<th>receiver</th>
					<th>threads processing</th>
					<xtags:forEach select="(receivers/receiver/idleStat)[1]/stat/summary/item">
						<th><xtags:valueOf select="@name"/></th>
					</xtags:forEach>
				</tr>
				<xtags:forEach select="receivers/receiver/idleStats/stat">
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

		<br/><br/><br/>
		<contentTable>
			<caption>Duration statistics per pipe (in ms)</caption>
			<tbody>
				<tr>
					<th>name</th>
					<xtags:forEach select="(pipeline/pipeStats/stat)[1]/summary/item">
						<th><xtags:valueOf select="@name"/></th>
					</xtags:forEach>
				</tr>
				<xtags:forEach select="pipeline/pipeStats/stat">
					<tr alternatingRows="true">
						<td><xtags:valueOf select="@name"/></td>
						<xtags:forEach select="summary/item">
							<td align="right"><xtags:valueOf select="@value"/></td>
						</xtags:forEach>
					</tr>
				</xtags:forEach> <!-- pipeStats -->
			</tbody>
		</contentTable>

		<br/><br/><br/>
		<contentTable>
			<caption>Waiting statistics per pipe (for availability, in ms)</caption>
			<tbody>
				<tr>
					<th>name</th>
					<xtags:forEach select="(pipeline/waitStats/stat)[1]/summary/item">
						<th><xtags:valueOf select="@name"/></th>
					</xtags:forEach>
				</tr>
				<xtags:forEach select="pipeline/waitStats/stat">
					<tr alternatingRows="true">
						<td><xtags:valueOf select="@name"/></td>
						<xtags:forEach select="summary/item">
							<td align="right"><xtags:valueOf select="@value"/></td>
						</xtags:forEach>
					</tr>
				</xtags:forEach> <!-- pipeStats -->
			</tbody>
		</contentTable>

		<br/><br/><br/>
		<contentTable>
			<caption>Size statistics per pipe (in bytes)</caption>
			<tbody>
				<tr>
					<th>name</th>
					<xtags:forEach select="(pipeline/sizeStats/stat)[1]/summary/item">
						<th><xtags:valueOf select="@name"/></th>
					</xtags:forEach>
				</tr>
				<xtags:forEach select="pipeline/sizeStats/stat">
					<tr alternatingRows="true">
						<td><xtags:valueOf select="@name"/></td>
						<xtags:forEach select="summary/item">
							<td align="right"><xtags:valueOf select="@value"/></td>
						</xtags:forEach>
					</tr>
				</xtags:forEach> <!-- pipeStats -->
			</tbody>
		</contentTable>
	</xtags:forEach>
</page>