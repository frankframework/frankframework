<%@ page import="java.util.*" %>
<%@ page import="javax.naming.*" %>
<%@ page import="org.apache.commons.text.StringEscapeUtils"%>

<br/>
<br/>
<hr/>
<b>Debug info:</b>

    <h2>HttpServletRequest Properties</h2>

    <table width="100%" cellspacing="0" cellpadding="2" bgcolor="#000000" border="1">
       <tr bgcolor="#dddddd">
        <th>Property</th>
        <th>Value</th>
      </tr>
      <tr bgcolor="#eeeeee">
        <td>getServerInfo()</td>
        <td><%= application.getServerInfo() %></td>
      </tr>
      <tr bgcolor="#eeeeee">
        <td>getAuthType()</td>
        <td><%= request.getAuthType() %></td>
      </tr>
      <tr bgcolor="#eeeeee">
        <td>getContextPath()</td>
        <td><%= request.getContextPath() %></td>
      </tr>
      <tr bgcolor="#eeeeee">
        <td>getPathInfo()</td>
        <td><%= request.getPathInfo() %></td>
      </tr>
      <tr bgcolor="#eeeeee">
        <td>getPathTranslated()</td>
        <td><%= request.getPathTranslated() %></td>
      </tr>
      <tr bgcolor="#eeeeee">
        <td>getQueryString()</td>
        <td><%= request.getQueryString() %></td>
      </tr>
      <tr bgcolor="#eeeeee">
        <td>getRequestedSessionId()</td>
        <td><%= request.getRequestedSessionId() %></td>
      </tr>
      <tr bgcolor="#eeeeee">
        <td>getRequestURI()</td>
        <td><%= request.getRequestURI() %></td>
      </tr>
      <tr bgcolor="#eeeeee">
        <td>getServletPath()</td>
        <td><%= request.getServletPath() %></td>
      </tr>
      <tr bgcolor="#eeeeee">
        <td>full uri for actionmappings</td>
        <%      String ctxtPath=request.getContextPath();
		        String reqUri=request.getRequestURI();
		        reqUri=reqUri.substring(ctxtPath.length(),reqUri.length());
		        reqUri+="?"+request.getQueryString();
		%>
        <td><%= reqUri %></td>
      </tr>
      <tr bgcolor="#eeeeee">
        <td>full url</td>
        <td><%= request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort() +request.getContextPath()%>
        </td>
      </tr>


    </table>


    <h2>ServletRequest Properties</h2>

    <table width="100%" cellspacing="0" cellpadding="2" bgcolor="#000000" border="1">
      <tr bgcolor="#dddddd">
        <th>Property</th>
        <th>Value</th>
      </tr>
      <tr bgcolor="#eeeeee">
        <td>getCharacterEncoding()</td>
        <td><%= request.getCharacterEncoding() %></td>
      </tr>
      <tr bgcolor="#eeeeee">
        <td>getContentType()</td>
        <td><%= request.getContentType() %></td>
      </tr>
      <tr bgcolor="#eeeeee">
        <td>getLocale()</td>
        <td><%= request.getLocale() %></td>
      </tr>
      <tr bgcolor="#eeeeee">
        <td>getProtocol()</td>
        <td><%= request.getProtocol() %></td>
      </tr>
      <tr bgcolor="#eeeeee">
        <td>getRemoteAddr()</td>
        <td><%= request.getRemoteAddr() %></td>
      </tr>
      <tr bgcolor="#eeeeee">
        <td>getRemoteHost()</td>
        <td><%= request.getRemoteHost() %></td>
      </tr>
      <tr bgcolor="#eeeeee">
        <td>getScheme()</td>
        <td><%= request.getScheme() %></td>
      </tr>
      <tr bgcolor="#eeeeee">
        <td>getServerName()</td>
        <td><%= request.getServerName() %></td>
      </tr>
    </table>

    <h2>HTTP Headers</h2>

    <table width="100%" cellspacing="0" cellpadding="2" bgcolor="#000000" border="1">
      <tr bgcolor="#dddddd">
        <th>Property</th>
        <th>Value</th>
      </tr>
      <%
        for ( Enumeration enumeration = request.getHeaderNames(); enumeration.hasMoreElements(); ) {
          String headerName = (String) enumeration.nextElement();
      %>
      <tr bgcolor="#eeeeee">
        <td><%= headerName %></td>
        <td><%= request.getHeader( headerName ) %></td>
      </tr>
      <%
        }
      %>
    </table>

    <h2>Request Attributes</h2>

    <table width="100%" cellspacing="0" cellpadding="2" bgcolor="#000000" border="1">
      <tr bgcolor="#dddddd">
        <th>Property</th>
        <th>Value</th>
      </tr>
      <%
        for ( Enumeration enumeration = request.getAttributeNames(); enumeration.hasMoreElements(); ) {
          String attributeName = (String) enumeration.nextElement();
		  String attributeValue = StringEscapeUtils.escapeHtml4(request.getAttribute(attributeName).toString());
      %>
      <tr bgcolor="#eeeeee">
        <td><%= attributeName %></td>
        <td><%= attributeValue %></td>
      </tr>
      <%
        }
      %>
    </table>


    <h2>Request Parameters</h2>

    <table width="100%" cellspacing="0" cellpadding="2" bgcolor="#000000" border="1">
      <tr bgcolor="#dddddd">
        <th>Property</th>
        <th>Value</th>
      </tr>
      <%
        for ( Enumeration enumeration = request.getParameterNames(); enumeration.hasMoreElements(); ) {
          String parameterName = (String) enumeration.nextElement();
		  String parameterValue = StringEscapeUtils.escapeHtml4(request.getParameter(parameterName));
      %>
      <tr bgcolor="#eeeeee">
        <td><%= parameterName %></td>
        <td><%= parameterValue %></td>
      </tr>
      <%
        }
      %>
    </table>

    <h2>Session Attributes</h2>

    <table width="100%" cellspacing="0" cellpadding="2" bgcolor="#000000" border="1">
      <tr bgcolor="#dddddd">
        <th>Property</th>
        <th>Value</th>
      </tr>
      <%
        for ( Enumeration enumeration = session.getAttributeNames(); enumeration.hasMoreElements(); ) {
          String attributeName = (String) enumeration.nextElement();
      %>
      <tr bgcolor="#eeeeee">
        <td><%= attributeName %></td>
        <td><% try{ out.println(session.getAttribute( attributeName)); }catch (Exception e){} %></td>
      </tr>
      <%
        }
      %>
    </table>

<br/>

    <h2>Environment Entries</h2>
    <table width="100%" cellspacing="0" cellpadding="2" bgcolor="#000000" border="1">
    <tr bgcolor="#dddddd">
        <th>Property</th>
        <th>Value</th>
     </tr>
     <%

      try {
         InitialContext ic = new InitialContext();

         NamingEnumeration ne =
                      ic.listBindings("java:comp/env");

         while (ne.hasMore()) {
            Binding ncp = (Binding)ne.next();
            String objName = ncp.getName();
            Object objObj = ncp.getObject();

            out.println("<TR bgcolor=\"#eeeeee\"><TD>" + objName + "</TD>");
            out.print(
             "<TD>" + objObj.toString() + "</TD></TR>");
         }
         out.println("</TABLE></BODY></HTML>");

      } catch (Exception e) {
        out.println("Error occurred retrieving entries:"+e.getMessage());
      }
      %>
      </table>
