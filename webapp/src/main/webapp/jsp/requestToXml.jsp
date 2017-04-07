<%@ page import="nl.nn.adapterframework.util.AppConstants"%>
<%@ page import="nl.nn.adapterframework.webcontrol.ConfigurationServlet"%>
<%@ page import="nl.nn.adapterframework.configuration.IbisContext" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ page import="java.util.Enumeration" %>

<requestInfo>
	<html:base/></base>
	<errors>
		<html:errors/>
	</errors>
	<messages>
	<html:messages id="message">
		<bean:write name="message"/>
	</html:messages>
	</messages>

	<%
		ServletContext sc = request.getSession().getServletContext();
		String attributeKey = AppConstants.getInstance().getProperty(ConfigurationServlet.KEY_CONTEXT);
		IbisContext ibisContext = (IbisContext) sc.getAttribute(attributeKey);
	%>
	<upTime><![CDATA[<%=ibisContext.getUptime()%>]]></upTime>

	<httpServletRequest>
		<auth_type><![CDATA[<%=request.getAuthType()%>]]></auth_type>
		<contextpath><![CDATA[<%=request.getContextPath()%>]]></contextpath>
		<contextString><![CDATA[<%= request.getQueryString()%>]]></contextString>
		<requestedSessionID><![CDATA[<%= request.getRequestedSessionId() %>]]></requestedSessionID>
		<servletPath><![CDATA[<%= request.getServletPath() %>]]></servletPath>
		<pathInfo><![CDATA[<%=request.getPathInfo()%>]]></pathInfo>
		<%-- determine full Request URI --%>
		<%-- -------------------------- --%>
		<%     String ctxtPath=request.getContextPath();
			   String reqUri=request.getRequestURI();
			   String queryString=request.getQueryString();
			   
			   if (null!=queryString){
				   reqUri+="?"+request.getQueryString();
		   	   }
		%>

		<fullRequestUri><![CDATA[<%=reqUri%>]]></fullRequestUri>
	</httpServletRequest>
	<servletRequest>
		<serverInfo><![CDATA[<%= application.getServerInfo() %>]]></serverInfo>
		<serverName><%= request.getServerName() %></serverName>
		<locale><%=request.getLocale()%></locale>
		<remoteAddress><%=request.getRemoteAddr()%></remoteAddress>
		<remoteHost><%=request.getRemoteHost()%></remoteHost>
		<scheme><%=request.getScheme()%></scheme>
		<appUrl><%= request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort() +request.getContextPath()%></appUrl>
	</servletRequest>

	<requestParameters>
	
	     <%
	        for ( Enumeration enumeration = request.getParameterNames(); enumeration.hasMoreElements(); ) {
	          String attributeName = (String) enumeration.nextElement();
	      %>
	      <parameter name="<%= attributeName %>" encodedValue="<%=java.net.URLEncoder.encode(request.getParameter( attributeName ))%>"><![CDATA[<%= request.getParameter( attributeName ) %>]]></parameter>
	      <%
	        }
	       %>
	</requestParameters>
</requestInfo>