
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>


<page title="Execute a Jdbc query">

<html:xhtml/>
<html:form action="/executeJdbcQueryExecute.do">
<html:hidden property="action"/>

<table border="0" width="100%">
<tr>
	<td>
		Select a jms realm
	</td>
	<td>
	
		<html:select property="jmsRealm">	
			<html:options name="executeJdbcQueryForm" property="jmsRealms"/> 
		</html:select> <br/>
  	 	
	</td>
</tr>
<tr>
	<td>
		Select a query type
	</td>
	<td>
		<html:select property="queryType">	
			<html:options name="executeJdbcQueryForm" property="queryTypes"/> 
		</html:select> <br/>
	</td>
</tr>
<tr>
	<td>
		result type
	</td>
	<td>
		<html:select property="resultType">	
			<html:options name="executeJdbcQueryForm" property="resultTypes"/> 
		</html:select> <br/>
	</td>
</tr>
  <tr>
  	<td>Query</td>
  	<td><html:textarea property="query" rows="10" cols="580"/></td>
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

