<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>
 
<page title="Show Environment variables">
	
	<xtags:parse>
			<bean:write name="envVars" scope="request" filter="false"/>
	</xtags:parse>

	<xtags:forEach select="environmentVariables/propertySet">
		<br/><br/>
		<contentTable>
			<caption><xtags:valueOf select="@name"/></caption>
			<tbody>
				<tr>
					<subHeader>Property</subHeader>
					<subHeader>Value</subHeader>
				</tr>
				<xtags:forEach select="property" sort="@name">
					<tr>
						<td><xtags:valueOf select="@name"/></td>
						<td><xtags:valueOf select="."/></td>
					</tr>
				</xtags:forEach>
			</tbody>
		</contentTable>
	</xtags:forEach>
</page>