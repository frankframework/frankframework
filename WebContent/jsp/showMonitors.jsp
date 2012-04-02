<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>

<html:xhtml/>  
<page title="Show Monitors" refresh="showMonitors.do">

	<imagelink
		href="showEvents.do"
		type="showEvents"
		text="Show all Events"
		>
	</imagelink>
	<imagelink
		href="showMonitorExecute.do"
		type="add"
		alt="createMonitor"
		text="Create a New Monitor"
		>
		<parameter name="action">createMonitor</parameter>
	</imagelink>
	<imagelink
		href="showMonitorExecute.do"
		type="export"
		alt="exportConfig"
		text="Export Monitor Configuration"
		>
		<parameter name="action">exportConfig</parameter>
	</imagelink>
	<imagelink
		href="showMonitors.do"
		type="status"
		alt="Show Status XML"
		>
		<parameter name="action">getStatus</parameter>
	</imagelink>
	<html:form action="/showMonitorExecute.do" enctype="multipart/form-data">
		<html:hidden property="action" value="edit"/>
		New Configuration File: <html:file property="configFile" title="Browse for ConfigFile"/>
		<br/>
		<br/>
		<contentTable>
			<caption>Monitors</caption>
			<tbody>
				<tr>
					<th colspan="4">Monitors</th><th colspan="5">State</th><th colspan="7">Triggers</th>
				</tr>
				<tr>
					<th>Action</th>
					<th>Notifications</th>
					<th>Name</th>
					<th>Type</th>
					
					<th>Raised</th>
					<th>Changed</th>
					<th>Hits</th>
					<th>Source</th>
					<th>Severity</th>
						
					<th>Action</th>
					<th>Type</th>
					<th>EventCodes</th>
					<th>Sources</th>
					<th>Severity</th>
					<th>Threshold</th>
					<th>Period</th>
				</tr>
	
 	<logic:iterate id="m" indexId="index"  name="MonitorForm"  property="monitorManager.monitors" >
 		<bean:size id="triggerCount" name="m" property="triggers"/>
		<tr>
			<xtags:element name="td">
				<xtags:attribute name="rowspan"><bean:write name="triggerCount"/></xtags:attribute>
				<imagelink 
					href="showMonitorExecute.do"
					type="delete"
					alt="delete monitor"
					>
					<parameter name="action">deleteMonitor</parameter>
					<parameter name="index"><bean:write name="index"/></parameter>
				</imagelink>
				<imagelink 
					href="editMonitor.do"
					type="edit"
					alt="edit"
					>
					<parameter name="action">edit</parameter>
					<parameter name="index"><bean:write name="index"/></parameter>
				</imagelink>
			</xtags:element>
			<xtags:element name="td">
				<xtags:attribute name="rowspan"><bean:write name="triggerCount"/></xtags:attribute>
					<table>
					 	<logic:iterate id="d" indexId="dIndex" name="MonitorForm" property="monitorManager.destinations" >
					 		<tr><td>
								<html:multibox name="MonitorForm" property="selDestinations" >
								<bean:write name="index" />,<bean:write name="d" property="key" />
								</html:multibox>
								<bean:write name="d" property="key" />
							</td></tr>
						</logic:iterate>
					</table>
			</xtags:element>
			<xtags:element name="td">
				<xtags:attribute name="rowspan"><bean:write name="triggerCount"/></xtags:attribute>
				<bean:write name="m" property="name" />
			</xtags:element>
			<xtags:element name="td">
				<xtags:attribute name="rowspan"><bean:write name="triggerCount"/></xtags:attribute>
				<bean:write name="m" property="type" />
			</xtags:element>
			
			<xtags:element name="td">
				<xtags:attribute name="rowspan"><bean:write name="triggerCount"/></xtags:attribute>
				<logic:equal name="m" property="raised" value="true" >
					<img src="images/raised.gif" title="raised"/>
					(<imagelink 
						href="showMonitorExecute.do"
						type="clear"
						alt="clear status"
						>
						<parameter name="action">clearMonitor</parameter>
						<parameter name="index"><bean:write name="index" /></parameter>
					</imagelink>)
				</logic:equal>
				<logic:notEqual name="m" property="raised" value="true" >
					<img src="images/cleared.gif" title="clear"/>
					(<imagelink 
						href="showMonitorExecute.do"
						type="raise"
						alt="raise"
						>
						<parameter name="action">raiseMonitor</parameter>
						<parameter name="index"><bean:write name="index" /></parameter>
					</imagelink>)
				</logic:notEqual>
			</xtags:element>
			<xtags:element name="td">
				<xtags:attribute name="rowspan"><bean:write name="triggerCount"/></xtags:attribute>
				<bean:write name="m" property="stateChangeDtStr" />
			</xtags:element>
			<xtags:element name="td">
				<xtags:attribute name="rowspan"><bean:write name="triggerCount"/></xtags:attribute>
				<logic:notEmpty name="m" property="lastHitStr">
					<bean:write name="m" property="additionalHitCount" /> since last raise;<br/>
					last hit <bean:write name="m" property="lastHitStr" /><br/>
				</logic:notEmpty>
			</xtags:element>
			<xtags:element name="td">
				<xtags:attribute name="rowspan"><bean:write name="triggerCount"/></xtags:attribute>
				<logic:notEmpty name="m" property="alarmSource">
					<bean:write name="m" property="alarmSource.adapter.name" /> / 
					<bean:write name="m" property="alarmSource.eventSourceName" />
				</logic:notEmpty>
			</xtags:element>
			<xtags:element name="td">
				<xtags:attribute name="rowspan"><bean:write name="triggerCount"/></xtags:attribute>
				<bean:write name="m" property="alarmSeverity" />
			</xtags:element>

			<logic:greaterThan name="triggerCount" value="0">
		 		<td>
					<imagelink 
						href="editMonitorExecute.do"
						type="delete"
						alt="delete trigger"
						>
						<parameter name="action">deleteTrigger</parameter>
						<parameter name="index"><bean:write name="index" /></parameter>
						<parameter name="triggerIndex">0</parameter>
						<parameter name="return">showmonitors</parameter>
					</imagelink>
					<imagelink 
						href="editTrigger.do"
						type="edit"
						alt="edit"
						>
						<parameter name="action">editTrigger</parameter>
						<parameter name="index"><bean:write name="index" /></parameter>
						<parameter name="triggerIndex">0</parameter>
						<parameter name="return">showmonitors</parameter>
					</imagelink>
		 		</td>
		 		
		 		<td><bean:write name="m" property="triggers[0].type"/></td>
		 		<td>
				 	<logic:iterate id="e" indexId="eindex"  name="m"  property="triggers[0].eventCodes" >
		 				<bean:write name="e" /><br/> 
		 			</logic:iterate>
		 		</td>
		 		<td>
				<logic:equal name="m"  property="triggers[0].filterOnAdapters" value="true">
					<logic:equal name="m"  property="triggers[0].filterExclusive" value="true">
					Exclude
					</logic:equal>
					Adapters:<br/>
				 	<logic:iterate id="s" indexId="sindex"  name="m"  property="triggers[0].adapters" >
		 				<bean:write name="s" /><br/> 
		 			</logic:iterate>
				</logic:equal>
				<logic:equal name="m"  property="triggers[0].filterOnLowerLevelObjects" value="true">
					<logic:equal name="m"  property="triggers[0].filterExclusive" value="true">
					Exclude
					</logic:equal>
					Sources:<br/>
				 	<logic:iterate id="s" indexId="sindex"  name="m"  property="triggers[0].sources" >
		 				<bean:write name="s" /><br/> 
		 			</logic:iterate>
				</logic:equal>
		 		</td>
		 		<td><bean:write name="m" property="triggers[0].severity"/></td>
		 		<td><bean:write name="m" property="triggers[0].threshold"/></td>
		 		<td><bean:write name="m" property="triggers[0].period"/></td>
			</logic:greaterThan>
			
		</tr>
	 	<logic:iterate id="t" indexId="tindex"  name="m"  property="triggers" >
			<logic:greaterThan name="tindex"  value="0">
		 		<tr>
			 		<td>
					<imagelink 
						href="editMonitorExecute.do"
						type="delete"
						alt="delete trigger"
						>
						<parameter name="action">deleteTrigger</parameter>
						<parameter name="index"><bean:write name="index" /></parameter>
						<parameter name="triggerIndex"><bean:write name="tindex" /></parameter>
						<parameter name="return">showmonitors</parameter>
					</imagelink>
					<imagelink 
						href="editTrigger.do"
						type="edit"
						alt="edit"
						>
						<parameter name="action">editTrigger</parameter>
						<parameter name="index"><bean:write name="index" /></parameter>
						<parameter name="triggerIndex"><bean:write name="tindex" /></parameter>
						<parameter name="return">showmonitors</parameter>
					</imagelink>
			 		</td>
			 		<td><bean:write name="t" property="type"/></td>
			 		<td>
					 	<logic:iterate id="e" indexId="eindex"  name="t"  property="eventCodes" >
			 				<bean:write name="e" /><br/> 
			 			</logic:iterate>
			 		</td>
			 		<td>
						<logic:equal name="t"  property="filterOnAdapters" value="true">
							<logic:equal name="t"  property="filterExclusive" value="true">
							Exclude
							</logic:equal>
							Adapters:<br/>
						 	<logic:iterate id="s" indexId="sindex"  name="t"  property="adapters" >
				 				<bean:write name="s" /><br/> 
				 			</logic:iterate>
						</logic:equal>
						<logic:equal name="t"  property="filterOnLowerLevelObjects" value="true">
							<logic:equal name="t"  property="filterExclusive" value="true">
							Exclude
							</logic:equal>
							Sources:<br/>
						 	<logic:iterate id="s" indexId="sindex"  name="t"  property="sources" >
				 				<bean:write name="s" /><br/> 
				 			</logic:iterate>
						</logic:equal>
			 		</td>
			 		<td><bean:write name="t" property="severity"/></td>
			 		<td><bean:write name="t" property="threshold"/></td>
			 		<td><bean:write name="t" property="period"/></td>
		 		</tr>
	 		</logic:greaterThan>
	 	</logic:iterate>
							
 	</logic:iterate>
 	
 				</tbody>
			</contentTable>
		<br/>
		<html:checkbox name="MonitorForm" property="enabled"/> Monitoring Enabled	
		<html:hidden name="MonitorForm" property="enabled" value="false"/>
		<br/>
		<br/>
		<html:submit tabindex="1" property="action">OK</html:submit>
	</html:form>		
</page>		
