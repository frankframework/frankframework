<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<!-- page test service -->

<html:xhtml/>

<page title="Test a Service">

<html:form action="/testServiceExecute.do" enctype="multipart/form-data">

<table border="0" width="100%">
<tr>
	<td>
		Select a service
	</td>
	<td>
		<html:select property="serviceName">	
			<html:options name="ServiceTestForm" property="services"/> 
		</html:select> <br/>
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
    	<html:xhtml/>
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


<!-- eof testservice -->

