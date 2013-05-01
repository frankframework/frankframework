
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ page import="org.apache.struts.action.DynaActionForm" %> 
<%@ page import="nl.nn.adapterframework.util.XmlUtils" %> 
<%@ taglib uri="/WEB-INF/xtags.tld" prefix="xtags" %>

<html:xhtml/>

<xtags:parse>
		<bean:write name="messages" scope="request" filter="false"/>
</xtags:parse>

<xtags:forEach select="messages">
	<xtags:variable id="pageTitle" select="concat('Browse ',@storageType,' of ',@object)"/>
	<page title="<%=pageTitle%>">

<html:form action="/browserExecute.do" >
	<html:hidden property="action"/>
	<html:hidden property="storageType"/>
	<html:hidden property="adapterName"/>
	<html:hidden property="receiverName"/>
	<html:hidden property="pipeName"/>
	
	<table border="0">
		<tr>
			<td>max num of messages displayed</td>
			<td><html:text property="maxMessages" size="6" maxlength="20"/></td>
		</tr>
		<tr>
			<td>skip first messages</td>
			<td><html:text property="skipMessages" size="6" maxlength="20"/></td>
		</tr>
		<tr>
			<td>filter on message text</td>
			<td colspan="4"><html:text property="messageTextMask" size="100" maxlength="200"/></td>
		</tr>
<!--
		<tr>
			<td>View messages as</td>
			<td>			
				<html:select property="viewAs">
					<html:options property="viewAsList" /> 
				</html:select>
			</td>
		</tr>	
-->		
		<tr>
			<td><html:submit property="submit" value="apply filters"/></td>
			<xtags:if test="@storageType!='messagelog'">
				<td><html:submit property="submit" value="resend selected"/></td>
				<td><html:submit property="submit" value="delete selected"/></td>
			</xtags:if>
			<td><html:submit property="submit" value="export selected"/></td>
			<td>
				<html:button onclick="setAll(this,'selected',true)" property="x" value="select all"/>
				<html:button onclick="setAll(this,'selected',false)" property="x" value="unselect all"/>
			</td>
		</tr>	
		
	</table>

		<contentTable>
			<caption><xtags:valueOf select="@messageCount"/> messages in <xtags:valueOf select="@storageType"/> of <xtags:valueOf select="@object"/></caption>
			<tbody>
				<tr>
					<subHeader>Actions</subHeader>
					<subHeader>No.</subHeader>
					<subHeader>Timestamp    [clip]</subHeader>
					<subHeader>Type</subHeader>
					<subHeader>Host</subHeader>
					<subHeader>Current ID</subHeader>
					<subHeader>Original ID</subHeader>
					<subHeader>CorrelationID</subHeader>
					<subHeader>Comment</subHeader>
					<subHeader>Expiry</subHeader>
					<subHeader>Label</subHeader>
				</tr>
				<tr>
					<td colspan="2">filters:</td>
					<td><html:text property="insertedAfter" size="25" maxlength="25"/><html:checkbox property="insertedAfterClip" /></td>
					<td><html:text property="typeMask" size="2" maxlength="2"/></td>
					<td><html:text property="hostMask" size="10" maxlength="180"/></td>
					<td><html:text property="currentIdMask" size="10" maxlength="180"/></td>
					<td><html:text property="messageIdMask" size="50" maxlength="180"/></td>
					<td><html:text property="correlationIdMask" size="50" maxlength="180"/></td>
					<td><html:text property="commentMask" size="50" maxlength="1000"/></td>
					<td/>
					<td><html:text property="labelMask" size="50" maxlength="180"/></td>
				</tr>
				<xtags:forEach select="message">
					<tr  alternatingRows="true">
						<td>
						<html:multibox property="selected" >
						  <xtags:valueOf select="@id"/>
						</html:multibox>
							<xtags:if test="@type='E'">
								<imagelink 
									href="browserExecute.do"
									type="delete"
									alt="delete message from errorQueue"
									>
									<parameter name="storageType"><xtags:valueOf select="../@storageType"/></parameter>
									<parameter name="action">deletemessage</parameter>
									<parameter name="adapterName"><xtags:valueOf select="../@adapterName"/></parameter>
									<parameter name="receiverName"><xtags:valueOf select="../@receiverName"/></parameter>
									<parameter name="pipeName"><xtags:valueOf select="../@pipeName"/></parameter>
									<parameter name="messageId"><xtags:valueOf select="@id"/></parameter>
								</imagelink>
								<imagelink 
									href="browserExecute.do"
									type="resend"
									alt="process the message again in the adapter"
									>
									<parameter name="storageType"><xtags:valueOf select="../@storageType"/></parameter>
									<parameter name="action">resendmessage</parameter>
									<parameter name="adapterName"><xtags:valueOf select="../@adapterName"/></parameter>
									<parameter name="receiverName"><xtags:valueOf select="../@receiverName"/></parameter>
									<parameter name="pipeName"><xtags:valueOf select="../@pipeName"/></parameter>
									<parameter name="messageId"><xtags:valueOf select="@id"/></parameter>
								</imagelink>
							</xtags:if>
							<imagelink 
								href="browser.do"
								type="showashtml"
								alt="show the message as html"
								newwindow="true"
								>
								<parameter name="storageType"><xtags:valueOf select="../@storageType"/></parameter>
								<parameter name="action">showmessage</parameter>
								<parameter name="adapterName"><xtags:valueOf select="../@adapterName"/></parameter>
								<parameter name="receiverName"><xtags:valueOf select="../@receiverName"/></parameter>
								<parameter name="pipeName"><xtags:valueOf select="../@pipeName"/></parameter>
								<parameter name="messageId"><xtags:valueOf select="@id"/></parameter>
								<parameter name="type">html</parameter>
							</imagelink>
							<imagelink 
								href="browser.do"
								type="showastext"
								alt="show the message as text"
								newwindow="true"
								>
								<parameter name="storageType"><xtags:valueOf select="../@storageType"/></parameter>
								<parameter name="action">showmessage</parameter>
								<parameter name="adapterName"><xtags:valueOf select="../@adapterName"/></parameter>
								<parameter name="receiverName"><xtags:valueOf select="../@receiverName"/></parameter>
								<parameter name="pipeName"><xtags:valueOf select="../@pipeName"/></parameter>
								<parameter name="messageId"><xtags:valueOf select="@id"/></parameter>
								<parameter name="type">text</parameter>
							</imagelink>
						</td>			
						<td><xtags:valueOf select="@pos"/></td>
						<td><xtags:valueOf select="@insertDate"/></td>
						<td><xtags:valueOf select="@type"/></td>
						<td><xtags:valueOf select="@host"/></td>
						<td><xtags:valueOf select="@id"/></td>
						<td><xtags:valueOf select="@originalId"/></td>
						<td><xtags:valueOf select="@correlationId"/></td>
						<td><xtags:valueOf select="@comment"/></td>
						<td><xtags:valueOf select="@expiryDate"/></td>
						<td><xtags:valueOf select="@label"/></td>
					</tr>
				</xtags:forEach>
			</tbody>
		</contentTable>
</html:form>
	</page>
</xtags:forEach>


