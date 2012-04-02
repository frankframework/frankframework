<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%@ page import="nl.nn.adapterframework.monitoring.Trigger" %>
<%@ page import="nl.nn.adapterframework.webcontrol.action.EditTrigger" %>


<html:xhtml/>
<page title="Edit a Trigger">



<html:form action="/editTriggerExecute.do" >
<html:hidden property="index"/>
<html:hidden property="return"/>
<html:hidden property="triggerIndex"/>

<table border="0" width="100%">
	<tr><td>Monitor</td><td><bean:write name="MonitorForm" property="monitor.name" /></td></tr>
	<tr>
		<td>Type</td>
		<td>
			<html:select property="trigger.type">	
				<html:options name="MonitorForm" property="triggerTypes" /> 
			</html:select>
		</td>
	</tr>
	<tr>
		<td rowspan="2">Events</td>
		<td rowspan="2">
			select a set of events:<br/>
			<html:select property="trigger.eventCodes" multiple="true" size="8" >	
				<html:options name="MonitorForm" property="eventCodes" /> 
			</html:select>
		</td>
		<td>
			<html:submit property="action" value="<%= EditTrigger.LABEL_FILTER_EVENTS2ADAPTERS %>" />
		</td>
	</tr>
	<tr>
		<td>
			<html:submit property="action" value="<%= EditTrigger.LABEL_FILTER_EVENTS2SOURCES %>"/>
		</td>
	</tr>
	<tr>
		<td>Source filtering</td>
		<td>
			<html:radio property="trigger.sourceFiltering" value="<%= Integer.toString(Trigger.SOURCE_FILTERING_NONE) %>">none</html:radio>	
			<html:radio property="trigger.sourceFiltering" value="<%= Integer.toString(Trigger.SOURCE_FILTERING_BY_ADAPTER) %>">by adapter</html:radio>	
			<html:radio property="trigger.sourceFiltering" value="<%= Integer.toString(Trigger.SOURCE_FILTERING_BY_LOWER_LEVEL_OBJECT) %>">by individual source</html:radio>	
		</td>
	</tr>
	<tr>
		<td rowspan="2">Adapters</td>
		<td rowspan="2">
			select a set of adapters:<br/>
		
			<%
				boolean adaptersReadonly=false; // geen readonly gedoe
			%>
			<logic:equal name="MonitorForm" property="trigger.filterOnAdapters" value="true">
				<%
					adaptersReadonly=false;
				%>
			</logic:equal>
			<html:select name="MonitorForm" property="selAdapters" multiple="true" size="8" disabled="<%= adaptersReadonly %>" >	
				<html:options name="MonitorForm" property="adapters" /> 
			</html:select>
		</td>
		<td>
			<html:submit property="action" value="<%= EditTrigger.LABEL_FILTER_ADAPTERS2EVENTS %>"/>
		</td>
	</tr>
	<tr>
		<td>
			<html:submit property="action" value="<%= EditTrigger.LABEL_FILTER_ADAPTERS2SOURCES %>"/>
		</td>
	</tr>
	<tr>
		<td rowspan="2">Sources</td>
		<td rowspan="2">
			select a set of event sources:<br/>
			<%
				boolean sourcesReadonly=false; // geen readonly gedoe
			%>
			<logic:equal name="MonitorForm" property="trigger.filterOnLowerLevelObjects" value="true">
				<%
					sourcesReadonly=false;
				%>
			</logic:equal>
			<html:select name="MonitorForm" property="selSources"  multiple="true" size="8" disabled="<%= sourcesReadonly %>" >	
				<html:options name="MonitorForm" property="sources" /> 
			</html:select>
		</td>
		<td>
			<html:submit property="action" value="<%= EditTrigger.LABEL_FILTER_SOURCES2EVENTS %>"/>
		</td>
	</tr>
	<tr>
		<td>
			<html:submit property="action" value="<%= EditTrigger.LABEL_FILTER_SOURCES2ADAPTERS %>"/>
		</td>
	</tr>
	<tr>
		<td>Filter sources exclusive</td>
		<td>
			<html:checkbox property="trigger.filterExclusive" />
			<html:hidden property="trigger.filterExclusive" value="false"/>
		</td>
	</tr>	
	<tr>
		<td>Severity</td>
		<td>
			<html:select property="trigger.severity">	
				<html:options name="MonitorForm" property="severities" /> 
			</html:select>
		</td>
	</tr>
	<tr>
		<td>Threshold</td>
		<td>
			<html:text property="trigger.threshold" />	
		</td>
	</tr>
	<tr>
		<td>Period</td>
		<td>
			<html:text property="trigger.period" />	
		</td>
	</tr>
	
</table>
<br/>
<html:submit tabindex="1" property="action">OK</html:submit>
<html:submit tabindex="2" property="action">Apply</html:submit>
<html:cancel tabindex="3">Cancel</html:cancel>
</html:form>



</page>

