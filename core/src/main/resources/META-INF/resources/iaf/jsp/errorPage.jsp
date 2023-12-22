<%@ page language="java" import="java.io.*" isErrorPage="true"%>
<%@ page import="org.frankframework.util.AppConstants"%>
<html>
	<head>
		<title>Server Error: <%= exception.getMessage() %></title>
	</head>
	<body>
		<!-- exception status code -->
		<b style="color: red">Status Code:</b>
		<span style="color: navy; font-weight: bold; font-size: 12pt"><%= request.getAttribute("javax.servlet.error.status_code")+"" %></span>
		<br>
		<br>(If
		<b>500</b>, see server/console log for details. If
		<b>404</b>, check the page's URL - perhaps a CAse eRrOr has occurred.)
		<br>

		<p>Please notify the administrator or check the application logs for more information.</p>

<%
String dtapStage = AppConstants.getInstance().getProperty("dtap.stage");
if (!"ACC".equals(dtapStage) && !"PRD".equals(dtapStage)) {
%>
		<br>
		<h2>Server Error</h2>
		<p>An error occurred processing your request:</p>
		<font face="Verdana" color="red" size="3"> <b><em><%= exception.getMessage() %></em></b></font>
		<hr/>
		<h3>Your request cannot be completed. The server got the following error.</h3>
		<p />
		<p />
		<pre><% exception.printStackTrace(new PrintWriter(out)); %></pre>

		<jsp:include page="debuginfo.jsp" flush="true" />
<% } %>
	</body>
</html>

