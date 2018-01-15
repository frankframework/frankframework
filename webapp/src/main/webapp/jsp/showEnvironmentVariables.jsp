<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>
<%@ page import="org.apache.log4j.Logger"%>
<%@ page import="org.apache.log4j.Level"%>
<%@ page import="nl.nn.adapterframework.util.AppConstants"%>
<%@ page import="nl.nn.adapterframework.util.XmlUtils" %>

<html:xhtml/>
 
<page title="Show Environment variables: <% out.write(XmlUtils.encodeChars((String)session.getAttribute("configurationName"))); %>">

	<html:form action="/logHandler.do">
		<contentTable>
			<caption>Dynamic parameters</caption>
			<tbody>
				<tr>
					<td>
						Root Log level</td>
					<td>
						<html:select property="logLevel" >	
							<html:option value="DEBUG"/>
							<html:option value="INFO"/>
							<html:option value="WARN"/>
							<html:option value="ERROR"/>
						</html:select> <br/>
					</td>
				</tr>
				<!-- todo: appconstants lezen en default zetten. -->
				<tr>
					<td>Log intermediary results</td>
					<td>
							<html:checkbox property="logIntermediaryResults"/>
					</td>
				</tr>
				<tr>
					<td>Length log records</td>
					<td>
							<html:text property="lengthLogRecords" size="8" maxlength="16"/>
					</td>
				</tr>

				<tr>
					<td>
						<html:reset>reset</html:reset>
					</td>
					<td>
						<html:submit>send</html:submit>
					</td>
				</tr>
			</tbody>
		</contentTable>
	</html:form>

	<br/>
	<br/>

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
							href="showEnvironmentVariables.do?configuration=<%=java.net.URLEncoder.encode(configuration)%>"
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

	<xtags:parse>
			<bean:write name="envVars" scope="request" filter="false"/>
	</xtags:parse>

	<div class="tabpanel">
		<% int pos1=0; %>
		<xtags:forEach select="environmentVariables/propertySet">
			<% pos1++; %>
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
			<br/>
		</xtags:forEach>
	</div>
</page>