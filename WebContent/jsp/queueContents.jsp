
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ page import="org.apache.struts.action.DynaActionForm" %> 



<page title="Browse a queue or topic">


<%
DynaActionForm f = (DynaActionForm) session.getAttribute("browseQueueForm");
Object o = f.get("messages");
String queueName = (String) f.get("destinationName");
java.util.List l = new java.util.ArrayList();
boolean showPayload;
if (f.get("showPayload")!=null) {
showPayload=((Boolean)f.get("showPayload")).booleanValue();
} else showPayload=false;

String count = "0";

if (o != null) {
	l = (java.util.List) f.get("messages");
	count = Integer.toString(l.size());
}	
else {
	count = (String) f.get("numberOfMessages");
}
count = (String) f.get("numberOfMessages");
%>
		<contentTable>
				<caption>queue <%=queueName%> with <%= count%> messages</caption>
				<tbody>
				<tr>
					<th>No.</th>
					<th>Date/Time</th>
					<th>MessageID</th>
					<th>CorrelationID</th>
					<% if (showPayload) {%>
					<th>PayLoad</th>
					<%}%>
				</tr>
<%
for (int i=0;i<l.size();i++) {
	String msg="";
	try {
		msg = ((javax.jms.TextMessage) l.get(i)).getText();
	}
	catch (Exception e) {
		msg="Could not display message: "+e.getMessage();
	}
%>
<tr  alternatingRows="true"><td><%=i+1%></td>
<td><%=new java.util.Date(((javax.jms.TextMessage) l.get(i)).getJMSTimestamp()).toString()%></td>
<td><%=((javax.jms.TextMessage) l.get(i)).getJMSMessageID()%></td>
<td><%=((javax.jms.TextMessage) l.get(i)).getJMSCorrelationID()%></td>
<% if (showPayload) {%>
		<td>
		<![CDATA[<%=msg%>]]></td>	
<%}%>

</tr>
<%}%>
			
<br/>
</tbody>
</contentTable>



</page>

