<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>
<%@ page import="org.apache.log4j.Logger"%>
<%@ page import="org.apache.log4j.Level"%>
<%@ page import="nl.nn.adapterframework.util.AppConstants"%>
<%@ page import="nl.nn.adapterframework.util.XmlUtils" %>

<page title="Show configuration: <% out.write(XmlUtils.encodeChars((String)session.getAttribute("configurationName"))); %>">

	<% String showAs = null; %>
	<% if (AppConstants.getInstance().getBoolean("showConfiguration.original", false)) { %>
		<% showAs = "showastext"; %>
	<% } else { %>
		<% showAs = "showashtml"; %>
	<% } %>
	<xtags:parse>
		<% out.write(XmlUtils.replaceNonValidXmlCharacters(request.getAttribute("configurations").toString())); %>
	</xtags:parse>
	<xtags:if test="count(//configuration) > 1">
		<ul class="tab">
			<xtags:forEach select="//configuration" sort="@nameUC">
				<xtags:variable id="configuration" select="."/>
				<% if (configuration.equals(session.getAttribute("configurationName"))) { %>
					<li class="active">
						<% out.write(XmlUtils.encodeChars(configuration)); %>
					</li>
				<% } else { %>
					<li>
						<a
							href="showConfiguration.do?configuration=<%=java.net.URLEncoder.encode(configuration)%>"
							alt="<% out.write(XmlUtils.encodeChars(configuration)); %>"
							text="<% out.write(XmlUtils.encodeChars(configuration)); %>"
							>
							<% out.write(XmlUtils.encodeChars(configuration)); %>
						</a>
					</li>
				<% } %>
			</xtags:forEach>
		</ul>
	</xtags:if>
	<div class="tabpanel">
		<br/>
		<ul class="tab">
			<% if (AppConstants.getInstance().getBoolean("showConfiguration.original", false)) { %>
				<li id="showconfig">
					<a
						href="configHandler.do?action=showloadedconfig"
						alt="showloadedconfig"
						text="showloadedconfig">
						showloadedconfig
					</a>
				</li>
			<% } else { %>
				<li class="active" id="showconfig">showloadedconfig</li>
			<% } %>
			<% if (AppConstants.getInstance().getBoolean("showConfiguration.original", false)) { %>
				<li class="active" id="showconfig">showoriginalconfig</li>
			<% } else { %>
				<li>
					<a id="showconfig"
						href="configHandler.do?action=showoriginalconfig"
						alt="showoriginalconfig"
						text="showoriginalconfig">
						showoriginalconfig
					</a>
				</li>
			<% } %>
		</ul>
		<pre><bean:write name="configXML" scope="request" filter="true"/></pre>
	</div>
</page>