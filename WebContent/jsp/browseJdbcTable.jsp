<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>


<page title="Browse a Jdbc table">

<html:xhtml/>
<html:form action="/browseJdbcTableExecute.do" focus="browse">
<html:hidden property="action"/>

<table border="0" width="100%">
<tr>
	<td>Select a jms realm</td>
	<td>
		<html:select property="jmsRealm">	
			<html:options name="browseJdbcTableForm" property="jmsRealms"/> 
		</html:select> <br/>
	</td>
</tr>
<tr>
	<td>Table name</td>
	<td><html:text property="tableName" size="20" maxlength="40"/>
	<img src="images/smallhelp.gif" alt="choose user_tables to select available tablenames.&#13;choose user_sequences to view available sequences" title="choose user_tables to select available tablenames.&#13;choose user_sequences to view available sequences"/></td>
</tr>
<tr>
	<td>Where</td>
	<td><html:text property="where" size="40" maxlength="80"/></td>
</tr>
<tr>
	<td>Order</td>
	<td><html:text property="order" size="40" maxlength="80"/></td>
</tr>
<tr>
	<td>Number of rows only</td>
	<td><html:checkbox property="numberOfRowsOnly"/></td>
</tr>
<tr>
	<td>Rownum min</td>
	<td><html:text property="rownumMin" size="8" maxlength="16"/></td>
</tr>
<tr>
	<td>Rownum max</td>
	<td><html:text property="rownumMax" size="8" maxlength="16"/></td>
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