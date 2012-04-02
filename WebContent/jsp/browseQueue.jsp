
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>


<page title="Browse a queue or topic">

<html:xhtml/>
<html:form action="/browseQueueExecute.do">
<html:hidden property="action"/>

<table border="0" width="100%">
<tr>
	<td>
		Select a jms realm
	</td>
	<td>
	
		<html:select property="jmsRealm">	
			<html:options name="browseQueueForm" property="jmsRealms"/> 
		</html:select> <br/>
  	 	
	</td>
</tr>
  <tr>
  	<td>Destination name</td>
  	<td><html:text property="destinationName" size="80" maxlength="180"/></td>
  </tr>
  <tr>
  	<td>Destination Type (QUEUE or TOPIC)</td>
  	<td><html:select property="destinationType" >
  			<html:option value="QUEUE"/>
  			<html:option value="TOPIC"/>
  		</html:select></td>
  </tr>

  <tr>
  	<td>Number of messages only</td>
  	<td><html:checkbox property="numberOfMessagesOnly"/></td>
  </tr>
<tr>
  	<td>Show payload</td>
  	<td><html:checkbox property="showPayload"/></td>
 </tr>  
  <tr>
    <td>
      <html:reset>reset</html:reset>
      
      <html:cancel>cancel</html:cancel>
    </td>
    <td>
      <html:submit>browse</html:submit>
    </td>
  </tr>

</table>
</html:form>



</page>

