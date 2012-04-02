<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%String pageTitle="";%>

<html:xhtml/>
<page title="Alter Tracing Properties">

<html:form action="/alterTracingPropertiesExecute.do" enctype="multipart/form-data">
<html:hidden property="action"/>

<table border="0" width="100%">
	<tr>
		<td>
			<html:textarea property="properties" rows="35" cols="60"/>
			<br/>
	  	</td>
	</tr>
	<tr>
		<td>
			<html:reset>reset</html:reset>
			<html:cancel>cancel</html:cancel>
			<html:submit>submit</html:submit>
		</td>
	</tr>
</table>
</html:form>
</page>