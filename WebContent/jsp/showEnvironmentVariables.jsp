<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>
 
<page title="Show Environment variables">
	
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