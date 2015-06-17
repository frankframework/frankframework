<%@ page import="nl.nn.adapterframework.util.AppConstants"%>

<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<xtags:valueOf select="@listenerClass"/>
<xtags:if test="@listenerDestination!=''">(<xtags:valueOf select="@listenerDestination"/>)</xtags:if>
<xtags:if test="@senderClass!=''">/<xtags:valueOf select="@senderClass"/>
	<xtags:if test="@senderDestination!=''">(<xtags:valueOf select="@senderDestination"/>)</xtags:if>
</xtags:if>
<xtags:if test="@threadCount!=0">
  <br/>( <xtags:valueOf select="@threadCount"/>/<xtags:valueOf select="@maxThreadCount"/>
  thread<xtags:if test="@maxThreadCount!=1">s</xtags:if><xtags:if test="@threadCountControllable='true'">,
  
	<imagelink
			href="adapterHandler.do"
			type="incthreads"
			alt="increase the maximum number of threads"
			text="inc"
		>
		<parameter name="action">incthreads</parameter>
		<parameter name="adapterName"><%=java.net.URLEncoder.encode(adapterName)%></parameter>
		<parameter name="receiverName"><%=java.net.URLEncoder.encode(receiverName)%></parameter>
	 </imagelink> -
	<imagelink
			href="adapterHandler.do"
			type="decthreads"
			alt="decrease the maximum number of threads"
			text="dec"
		>
		<parameter name="action">decthreads</parameter>
		<parameter name="adapterName"><%=java.net.URLEncoder.encode(adapterName)%></parameter>
		<parameter name="receiverName"><%=java.net.URLEncoder.encode(receiverName)%></parameter>
	 </imagelink>
	</xtags:if>
  )
</xtags:if>
