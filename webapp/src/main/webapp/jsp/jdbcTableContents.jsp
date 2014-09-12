<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>

<page title="Browse a Jdbc table">
	<xtags:parse>
		<bean:write name="DB2Xml" scope="request" filter="false"/>
	</xtags:parse>

	<xtags:forEach select="resultEnvelope">
		<text property="query" rows="10" cols="580">
			<xtags:valueOf select="request"/>
		</text>
		<br/>
		<br/>
		<contentTable>
			<caption>
				<xtags:valueOf select="request/@tableName"/>
			</caption>
			<tbody>
				<tr>
					<xtags:forEach select="result/fielddefinition/field">
						<xtags:variable id="name" select="@name"/>
						<th>
							<text>
								<xtags:valueOf select="$name"/>
							</text>
							<% if (name.startsWith("LENGTH ")) { %>
								<xtags:variable id="text" select="'to show the content of this field instead of the length Rownum max should equal Rownum min and should be greater than zero'"/>
								<img src="images/smallhelp.gif" alt="<%=text%>" title="<%=text%>"/>
							<% } %>
						</th>
					</xtags:forEach>
				</tr>
				<xtags:forEach select="result/rowset/row">
					<tr alternatingRows="true">
						<xtags:forEach select="field">
							<td>
								<xtags:valueOf select="."/>
							</td>
						</xtags:forEach>
					</tr>
				</xtags:forEach>
			</tbody>
		</contentTable>
	</xtags:forEach>
</page>		

