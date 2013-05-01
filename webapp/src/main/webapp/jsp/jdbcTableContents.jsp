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
						<th>
							<xtags:valueOf select="@name"/>
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

