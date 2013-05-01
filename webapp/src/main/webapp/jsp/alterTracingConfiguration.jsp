<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%String pageTitle="";%>

<html:xhtml/>
<page title="Alter Tracing Configuration">

<html:form action="/alterTracingConfigurationExecute.do" enctype="multipart/form-data">
<html:hidden property="action"/>

<table border="0" width="100%">
	<tr>
		<td>Adapter</td>
		<td>
			<html:text property="adapterName" size="50" readonly="true" style="color:gray;"/>
			<br/>
	  	</td>
	</tr>
	<tr>
		<td>Receiver</td>
		<td>
			<html:text property="receiverName" size="50" readonly="true" style="color:gray;"/>
			<br/>
	  	</td>
	</tr>
	<tr>
		<td>Pipe</td>
		<td>
			<html:text property="pipeName" size="50" readonly="true" style="color:gray;"/>
			<br/>
	  	</td>
	</tr>
	<tr>
		<td>BeforeEvent</td>
		<td>
			<html:text property="beforeEvent" size="5"/>
			<br/>
		</td>
	</tr>
	<tr>
		<td>AfterEvent</td>
		<td>
			<html:text property="afterEvent" size="5"/>
			<br/>
		</td>
	</tr>
	<tr>
		<td>ExceptionEvent</td>
		<td>
			<html:text property="exceptionEvent" size="5"/>
			<br/>
		</td>
	</tr>
	<tr>
		<td>
			<html:reset>reset</html:reset>
			<html:cancel>cancel</html:cancel>
		</td>
		<td>
			<html:submit>submit</html:submit>
		</td>
	</tr>
</table>
</html:form>
</page>