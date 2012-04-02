
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%String pageTitle="";%>

<html:xhtml/>
<page title="Test a PipeLine">


<html:form action="/testPipeLineExecute.do" enctype="multipart/form-data">
<html:hidden property="action"/>

<table border="0" width="100%">
	<tr>
		<td>Select an adapter</td>
		<td>
			<html:select property="adapterName">	
				<html:options name="PipeLineTestForm" property="adapters" /> 
			</html:select>
		</td>
	</tr>	

	<tr>
		<td>Message</td>
		<td><html:textarea property="message" rows="10" cols="580"/></td>
	</tr>	
	<tr>
		<td>Upload File</td>
		<td><html:file property="file"/></td>
	</tr>
	<tr>
		<td>Result</td>
		<td><html:textarea property="result" rows="10" cols="580"/></td>
	</tr>
	<tr>
		<td>State</td>
		<td><html:text property="state" size="80" maxlength="180"/></td>
		<br/>
	</tr>
	<tr>
		<td>
			<html:reset>reset</html:reset>
			<html:cancel>cancel</html:cancel>
		</td>
		<td><html:submit>send</html:submit></td>
	</tr>

	<tr><td> </td></tr>

	

</table>
</html:form>



</page>

