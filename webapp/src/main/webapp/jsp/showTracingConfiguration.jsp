<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>
<%@ page import="nl.nn.adapterframework.util.TracingUtil" %>

  
<page title="Show Tracing Configuration" refresh="showTracingConfiguration.do">
	<xtags:parse>
		<bean:write name="tracingConfiguration" scope="request" filter="false"/>
	</xtags:parse>

	<%   if (TracingUtil.isStarted()) { %>
		<img src="images/connected.gif" title="started"/>
	<% } else { %>
		<img src="images/disconnected.gif" title="stopped"/>
	<%}%>

	<imagelink 
			href="tracingHandler.do"
			type="stop"
			alt="stop"
			text="Stop tracing"
			>
			<parameter name="action">stoptracing</parameter>
	 </imagelink>
	<imagelink 
			href="tracingHandler.do"
			type="start"
			alt="start"
			text="Start tracing"
			>
			<parameter name="action">starttracing</parameter>
	 </imagelink>
	<imagelink 
			href="alterTracingProperties.do"
			type="configuration"
			alt="configuration"
			text="Alter properties"
			>
	 </imagelink>

	<xtags:forEach select="tracingConfiguration">
		<contentTable>
			<caption>Configured Adapters</caption>
			<tbody>
				<xtags:forEach select="registeredAdapters/adapter" sort="@nameUC">
					<xtags:variable id="adapterName" select="@name"/>
					<tr>
						<subHeader>Adapter</subHeader>
					</tr>
					<tr>
						<td>
							<xtags:valueOf select="$adapterName"/>
						</td>
					</tr>
					<tr>
						<td></td>
						<subHeader>Receiver</subHeader>
						<subHeader>beforeEvent</subHeader>
						<subHeader>afterEvent</subHeader>
						<subHeader>exceptionEvent</subHeader>
					</tr>
					<xtags:forEach select="receivers/receiver">
						<xtags:variable id="receiverName" select="@name"/>
						<tr>
							<td></td>
							<td>
								<xtags:valueOf select="$receiverName"/>
							</td>
							<td>
								<xtags:valueOf select="@beforeEvent"/>
							</td>
							<td>
								<xtags:valueOf select="@afterEvent"/>
							</td>
							<td>
								<xtags:valueOf select="@exceptionEvent"/>
							</td>
							<td>
								<a href="alterTracingConfiguration.do?adapter=<%=adapterName%>&amp;receiver=<%=receiverName%>">alter</a>
							</td>
						</tr>
					</xtags:forEach>
					<tr>
						<td></td>
						<subHeader>Pipe</subHeader>
						<subHeader>beforeEvent</subHeader>
						<subHeader>afterEvent</subHeader>
						<subHeader>exceptionEvent</subHeader>
					</tr>
					<xtags:forEach select="pipeline/pipe">
						<xtags:variable id="pipeName" select="@name"/>
						<tr>
							<td></td>
							<td>
								<xtags:valueOf select="$pipeName"/>
							</td>
							<td>
								<xtags:valueOf select="@beforeEvent"/>
							</td>
							<td>
								<xtags:valueOf select="@afterEvent"/>
							</td>
							<td>
								<xtags:valueOf select="@exceptionEvent"/>
							</td>
							<td>
								<a href="alterTracingConfiguration.do?adapter=<%=adapterName%>&amp;pipe=<%=pipeName%>">alter</a>
							</td>
						</tr>
					</xtags:forEach>
				</xtags:forEach>
			</tbody>
		</contentTable>
	</xtags:forEach>
</page>