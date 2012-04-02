<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<html:xhtml/>
<page title="Edit a Monitor">


<html:form action="/editMonitorExecute.do" >
<html:hidden property="index"/>
<html:hidden property="return" value="showmonitors"/>

<table border="0" width="100%">
	<tr><th>Monitor</th></tr>
	<tr>
		<td>Name</td>
		<td><html:text property="monitor.name" size="80" maxlength="180"/></td>
	</tr>
	<tr>
		<td>Notifications</td>
		<td>
		 	<logic:iterate id="d" indexId="dIndex" name="MonitorForm" property="monitorManager.destinations" >
				<html:multibox name="MonitorForm" property="monitor.destinations" >
				<bean:write name="d" property="key" />
				</html:multibox>
				<bean:write name="d" property="key" />
			</logic:iterate>
		</td>
	</tr>
	<tr>
		<td>Type</td>
		<td>
			<html:select property="monitor.type">	
				<html:options name="MonitorForm" property="eventTypes" /> 
			</html:select>
		</td>
	</tr>

	<tr><td> </td></tr>
	<tr><th>Triggers</th></tr>
	<tr>
		<td>
	 		<imagelink
				href="editMonitorExecute.do"
				type="add"
				alt="createTrigger"
				text="Create a New Trigger"
				>
				<parameter name="action">createTrigger</parameter>
				<parameter name="index"><bean:write name="MonitorForm" property="index" /></parameter>
			</imagelink>
		</td>
	</tr>

	<logic:iterate id="element" indexId="triggerIndex"  name="MonitorForm"  property="monitor.triggers" >
		<tr>
			<td>
				<imagelink 
					href="editMonitorExecute.do"
					type="delete"
					alt="delete trigger"
					>
					<parameter name="action">deleteTrigger</parameter>
					<parameter name="index"><bean:write name="MonitorForm" property="index" /></parameter>
					<parameter name="triggerIndex"><bean:write name="triggerIndex" /></parameter>
					<parameter name="return">editmonitor</parameter>
				</imagelink>
				<imagelink 
					href="editTrigger.do"
					type="edit"
					alt="edit"
					>
					<parameter name="action">editTrigger</parameter>
					<parameter name="index"><bean:write name="MonitorForm" property="index" /></parameter>
					<parameter name="triggerIndex"><bean:write name="triggerIndex"/></parameter>
					<parameter name="return">editmonitor</parameter>
				</imagelink>
			</td>
			<td>
				<table>
					<tr>
						<td>Type: <bean:write name="element" property="type"/></td>
				 		<td>EventCodes: 
						 	<logic:iterate id="e" indexId="eindex"  name="element"  property="eventCodes" >
				 				<bean:write name="e" />; 
				 			</logic:iterate>
				 		</td>
				 		<td>
				 			<logic:equal name="element"  property="filterOnAdapters" value="true">
					 			<logic:equal name="element"  property="filterExclusive" value="true">
					 				Exclude
					 			</logic:equal>
								Adapters:
							 	<logic:iterate id="s" indexId="sindex"  name="element"  property="adapters" >
					 				<bean:write name="s" />; 
					 			</logic:iterate>
							</logic:equal>
							<logic:equal name="element"  property="filterOnLowerLevelObjects" value="true">
					 			<logic:equal name="element"  property="filterExclusive" value="true">
					 				Exclude
					 			</logic:equal>
								Sources:
							 	<logic:iterate id="s" indexId="sindex"  name="element"  property="sources" >
					 				<bean:write name="s" />; 
					 			</logic:iterate>
							</logic:equal>
				 		</td>
						<td>Severity: <bean:write name="element" property="severity"/></td>
						<td>Threshold: <bean:write name="element" property="threshold"/></td>
						<td>Period: <bean:write name="element" property="period"/></td>
					</tr>
				</table>
			</td>
		</tr>
	</logic:iterate>
</table>
<br/>
<html:submit tabindex="1" property="action">OK</html:submit>
<html:submit tabindex="2" property="action">Apply</html:submit>
<html:cancel tabindex="3">Cancel</html:cancel>
</html:form>



</page>

