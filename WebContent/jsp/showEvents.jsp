<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<page title="Show Events">

	<contentTable>
		<caption>Events and Thrower Types</caption>
		<tbody>
			<tr><th>Event</th><th>Thrown by</th></tr>
		 	<logic:iterate id="e" name="MonitorForm" property="monitorManager.throwerTypesByEvent" >
				<tr>
					<td><bean:write name="e" property="key" /></td>
					<td>
					 	<logic:iterate id="v" name="e" property="value" >
					 		<bean:write name="v" property="name" /><br/>
					 	</logic:iterate>
					</td>
				</tr>
			</logic:iterate>
		</tbody>
	</contentTable>

	<contentTable>
		<caption>Events and Thrower</caption>
		<tbody>
			<tr><th>Event</th><th>Thrown by</th></tr>
		 	<logic:iterate id="e" name="MonitorForm" property="monitorManager.throwersByEvent" >
				<tr>
					<td><bean:write name="e" property="key" /></td>
					<td>
					 	<logic:iterate id="v" name="e" property="value" >
					 		<bean:write name="v" property="adapter.name" /> /
					 		<bean:write name="v" property="eventSourceName" /><br/>
					 	</logic:iterate>
					</td>
				</tr>
			</logic:iterate>
		</tbody>
	</contentTable>

	<contentTable>
		<caption>Thrower Types and Events</caption>
		<tbody>
			<tr><th>Object Type</th><th>Throws</th></tr>
		 	<logic:iterate id="t" name="MonitorForm" property="monitorManager.eventsByThrowerType" >
				<tr>
					<td><bean:write name="t" property="key" /></td>
					<td>
					 	<logic:iterate id="v" name="t" property="value" >
					 		<bean:write name="v" /><br/>
					 	</logic:iterate>
					</td>
				</tr>
			</logic:iterate>
		</tbody>
	</contentTable>

</page>

