<%@ page import="nl.nn.adapterframework.util.AppConstants"%>
<%@ page import="nl.nn.adapterframework.util.Misc"%>
<%@ page import="nl.nn.adapterframework.util.ProcessMetrics"%>
<%@ page import="nl.nn.adapterframework.util.XmlUtils"%>
<%@ page import="org.apache.commons.lang.builder.ToStringBuilder"%>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/xtags.tld" prefix="xtags" %>
<%@ page import="org.apache.struts.Globals"%>

<%@ page language="java" %>
 
<%
	String view=request.getParameter("view");

	String contenttype="text/html";
	String output=request.getParameter("output");
	if ("xml".equals(output)) {
		response.setContentType("text/xml;charset=UTF-8");
%>
		<page>
<%
			String attribute=request.getParameter("attribute");
			if (attribute!=null) {
				Object value=request.getAttribute(attribute);
				out.println("<attribute name=\""+attribute+"\" class=\""+value.getClass().getName()+"\">"+value+"</attribute>");
			} else {
				for(Enumeration enumeration=request.getAttributeNames();enumeration.hasMoreElements();) {
					String name=(String)enumeration.nextElement();
					Object value=request.getAttribute(name);
					String string;
					if (value instanceof String) {
						string = (String)value;
					} else {
						string = ToStringBuilder.reflectionToString(value);
					}
					if (name.contains(".") || name.equals("configXML")) {
						string = XmlUtils.encodeChars(string);
					}
					out.println("<attribute name=\""+name+"\" class=\""+value.getClass().getName()+"\">"+string+"</attribute>");
				}
				%>
				<%@ include file="requestToXml.jsp" %>
				<machineName><%=Misc.getHostname()%></machineName>
				<fileSystem>
					<totalSpace><%=Misc.getFileSystemTotalSpace()%></totalSpace>
					<freeSpace><%=Misc.getFileSystemFreeSpace()%></freeSpace>
				</fileSystem>
				<%=AppConstants.getInstance().toXml(true)%>
				<%=ProcessMetrics.toXml()%>
<%
			}
%>
		</page>
<%
	} else {
		response.setContentType("text/html;charset=UTF-8");
%>

<xtags:parse id="doc1">

	<abstractPage>

		<%@ include file="requestToXml.jsp" %>
		<machineName><%=Misc.getHostname()%></machineName>
		<fileSystem>
			<totalSpace><%=Misc.getFileSystemTotalSpace()%></totalSpace>
			<freeSpace><%=Misc.getFileSystemFreeSpace()%></freeSpace>
		</fileSystem>
		<%=AppConstants.getInstance().toXml(true)%>
		<%=ProcessMetrics.toXml()%>

		<%@ include file="menuBar.jsp" %>

		<% if (view.equals("/jsp/browseQueue.jsp")) {%>  
			<jsp:include page="/jsp/browseQueue.jsp" flush="true"/>
		<%} else if (view.equals("/jsp/queueContents.jsp")) {%>
			<jsp:include page="/jsp/queueContents.jsp" flush="true"/>		
		<%} else if (view.equals("/jsp/browseJdbcTable.jsp")) {%>  
			<jsp:include page="/jsp/browseJdbcTable.jsp" flush="true"/>
		<%} else if (view.equals("/jsp/jdbcTableContents.jsp")) {%>
			<jsp:include page="/jsp/jdbcTableContents.jsp" flush="true"/>		
		<%} else if (view.equals("/jsp/browse.jsp")) {%>
			<jsp:include page="/jsp/browse.jsp" flush="true"/>	
		<%} else if (view.equals("/jsp/sendJmsMessage.jsp")) {%>
			<jsp:include page="/jsp/sendJmsMessage.jsp" flush="true"/> 
		<%} else if (view.equals("/jsp/showAdapterStatistics.jsp")) {%>
			<jsp:include page="/jsp/showAdapterStatistics.jsp" flush="true"/>
		<%} else if (view.equals("/jsp/testService.jsp")) {%> 
			<jsp:include page="/jsp/testService.jsp" flush="true"/>
		<%} else if (view.equals("/jsp/showLogging.jsp")) {%>
			<jsp:include page="/jsp/showLogging.jsp" flush="true"/> 
		<%} else if (view.equals("/jsp/noConfig.jsp")) {%> 
			<jsp:include page="/jsp/noConfig.jsp" flush="true"/>
		<%} else if (view.equals("/jsp/noIbisContext.jsp")) {%> 
			<jsp:include page="/jsp/noIbisContext.jsp" flush="true"/>
		<%} else if (view.equals("/jsp/showSchedulerStatus.jsp")) {%>
			<jsp:include page="/jsp/showSchedulerStatus.jsp" flush="true"/>
		<%} else if (view.equals("/jsp/executeJdbcQuery.jsp")) {%>
			<jsp:include page="/jsp/executeJdbcQuery.jsp" flush="true"/> 
		<%} else if (view.equals("/jsp/showSecurityItems.jsp")) {%>
			<jsp:include page="/jsp/showSecurityItems.jsp" flush="true"/> 
		<%} else if (view.equals("/jsp/showMonitors.jsp")) {%>
			<jsp:include page="/jsp/showMonitors.jsp" flush="true"/> 
		<%} else if (view.equals("/jsp/showIbisstoreSummary.jsp")) {%>
			<jsp:include page="/jsp/showIbisstoreSummary.jsp" flush="true"/> 
		<%} else if (view.equals("/jsp/showEvents.jsp")) {%>
			<jsp:include page="/jsp/showEvents.jsp" flush="true"/> 
		<%} else if (view.equals("/jsp/editMonitor.jsp")) {%>
			<jsp:include page="/jsp/editMonitor.jsp" flush="true"/> 
		<%} else if (view.equals("/jsp/editTrigger.jsp")) {%>
			<jsp:include page="/jsp/editTrigger.jsp" flush="true"/> 
		<%} else if (view.equals("/jsp/testTool.jsp")) {%>
			<% response.sendRedirect("testtool"); %>
		<%} else {%>
			<page><%=view%> not configured in view.jsp</page>
		<%}%>

	</abstractPage>  

	</xtags:parse>
	<%=XmlUtils.getAdapterSite(doc1)%>
<%
	} 
%>

<%--<jsp:include page="debuginfo.jsp" flush="true"/>--%>

