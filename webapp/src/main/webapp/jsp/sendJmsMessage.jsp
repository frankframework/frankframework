
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>


<html:xhtml/>
<page title="Send a JMS message">


<html:form action="/sendJmsMessageExecute.do" enctype="multipart/form-data">
<html:hidden property="action"/>

<table border="0" width="100%">
<tr>
	<td>
		Select a jms realm
	</td>
	<td>
	
		<html:select property="jmsRealm">	
			<html:options name="sendJmsMessageForm" property="jmsRealms"/> 
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
  	<td>ReplyTo</td>
  	<td><html:text property="replyToName" size="80" maxlength="180"/></td>
  </tr>

  <tr>
  	<td>Persistent</td>
  	<td><html:checkbox property="persistent"/></td>
  </tr>
   <tr>
  	<td>Message</td>
  	<td><html:textarea property="message" rows="10" cols="580"/><br/>
  	    <html:file property="file"/>
  	</td>
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

