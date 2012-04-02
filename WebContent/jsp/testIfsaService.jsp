
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%String pageTitle="";%>

<html:xhtml/>
<page title="Call an IFSA Service">


<html:form action="/testIfsaServiceExecute.do" enctype="multipart/form-data">
<html:hidden property="action"/>

<table border="0" width="100%">
   <tr>
  	<td>Application ID</td>
  	<td><html:text property="applicationId" size="80" maxlength="180"/></td>
  </tr>
   <tr>
  	<td>Service ID</td>
  	<td><html:text property="serviceId" size="80" maxlength="180"/></td>
  </tr>
  <tr>
	<td>Message Protocol</td>
	<td>
		<html:select property="messageProtocol">	
			<html:options name="testIfsaServiceForm" property="messageProtocols" /> 
		</html:select> 
		<br/>
	</td>
  </tr>
  <tr>
  	<td>Message</td>
  	<td><html:textarea property="message" rows="10" cols="580"/><br/>
  		<html:file property="file"/></td>
   </tr>
   <tr>
  	<td>Result</td>
  	<td><html:textarea property="result" rows="10" cols="580"/></td>
  </tr>
	
  <tr>
    <td>
      <html:reset>reset</html:reset>
      
      <html:cancel>cancel</html:cancel>
    </td>
    <td>
      <html:submit>send</html:submit>
    </td>
  </tr>

</table>
</html:form>



</page>

