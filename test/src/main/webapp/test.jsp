<%
// Returns the request parameter as well as the sesssion id
String nr = request.getParameter("nr");
if(nr == null) nr = "0";
String sessionId = request.getSession().getId();
String result = "<session nr=\"" + nr + "\">" + sessionId + "</session>";
%>
<%=result%>