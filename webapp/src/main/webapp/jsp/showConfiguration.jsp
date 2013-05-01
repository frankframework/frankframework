<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>
<%@ page import="org.apache.log4j.Logger"%>
<%@ page import="org.apache.log4j.Level"%>
<%@ page import="nl.nn.adapterframework.util.AppConstants"%>
 
  <html:xhtml/>
<page title="Show configuration">

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
	
 	<pre>
			<bean:write name="configXML" scope="request" filter="true"/>
	</pre>

<br/><br/>

	<html:form action="/logHandler.do">
	<% Logger log = Logger.getRootLogger();
		  String curlevel=log.getLevel().toString();
	  %>

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