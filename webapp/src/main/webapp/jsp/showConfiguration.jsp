<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>
<%@ page import="org.apache.log4j.Logger"%>
<%@ page import="org.apache.log4j.Level"%>
<%@ page import="nl.nn.adapterframework.util.AppConstants"%>
<%@ page import="nl.nn.adapterframework.util.XmlUtils" %>
 
  <html:xhtml/>
<page title="Show configuration: <% out.write(XmlUtils.replaceNonValidXmlCharacters((String)request.getAttribute("configurationName"))); %>">

	<% if (AppConstants.getInstance().getBoolean("showConfiguration.original", false)) { %>
		<imagelink
				href="configHandler.do"
				type="showashtml"
				alt="showloadedconfig"
				text="Show loaded configuration"
				>
				<parameter name="action">showloadedconfig</parameter>
		 </imagelink>
	<% } else { %>
		<imagelink
				href="configHandler.do"
				type="showastext"
				alt="showoriginalconfig"
				text="Show original configuration"
				>
				<parameter name="action">showoriginalconfig</parameter>
		 </imagelink>
	<% } %>

	<xtags:parse>
		<% out.write(XmlUtils.replaceNonValidXmlCharacters(request.getAttribute("configurations").toString())); %>
	</xtags:parse>
	<xtags:if test="count(//configuration) > 1">
		<xtags:forEach select="//configuration">
			<xtags:variable id="configuration" select="."/>
			<imagelink
				href="showConfiguration.do"
				type="showastext"
				alt="<%=configuration%>"
				text="<%=configuration%>"
				>
				<parameter name="configuration"><xtags:valueOf select="." /></parameter>
			</imagelink>
		</xtags:forEach>
	</xtags:if>

 	<pre>
			<bean:write name="configXML" scope="request" filter="true"/>
	</pre>

<br/><br/>

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
</page>