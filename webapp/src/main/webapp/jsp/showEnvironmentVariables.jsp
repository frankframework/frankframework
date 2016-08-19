<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>
<%@ page import="nl.nn.adapterframework.util.XmlUtils" %>
 
<page title="Show Environment variables: <% out.write(XmlUtils.encodeChars((String)session.getAttribute("configurationName"))); %>">

	<xtags:parse>
		<% out.write(XmlUtils.replaceNonValidXmlCharacters(request.getAttribute("configurations").toString())); %>
	</xtags:parse>

	<xtags:if test="count(//configuration) > 1">
		<xtags:forEach select="//configuration">
			<xtags:variable id="configuration" select="."/>
			<% if (configuration.equals(session.getAttribute("configurationName"))) { %>
				<image
					type="showastext"
					alt="<% out.write(XmlUtils.encodeChars(configuration)); %>"
					text="<% out.write(XmlUtils.encodeChars(configuration)); %>"
				/>
			<% } else { %>
				<imagelink
					href="showEnvironmentVariables.do"
					type="showastext"
					alt="<% out.write(XmlUtils.encodeChars(configuration)); %>"
					text="<% out.write(XmlUtils.encodeChars(configuration)); %>"
					>
					<parameter name="configuration"><%=java.net.URLEncoder.encode(configuration)%></parameter>
				</imagelink>
			<% } %>
		</xtags:forEach>
	</xtags:if>

	<xtags:parse>
			<bean:write name="envVars" scope="request" filter="false"/>
	</xtags:parse>

	<% int pos1=0; %>
	<xtags:forEach select="environmentVariables/propertySet">
		<% pos1++; %>
		<br/><br/>
		<contentTable>
			<caption><xtags:valueOf select="@name"/></caption>
			<tbody>
				<tr>
					<subHeader>Id</subHeader>
					<subHeader>Property</subHeader>
					<subHeader>Value</subHeader>
				</tr>
				<% int pos2=0; %>
				<xtags:forEach select="property" sort="@name">
				<% pos2++; %>
					<tr>
						<td><% out.print(pos1+"-"+pos2); %></td>
						<td><xtags:valueOf select="@name"/></td>
						<td><xtags:valueOf select="."/></td>
					</tr>
				</xtags:forEach>
			</tbody>
		</contentTable>
	</xtags:forEach>
</page>