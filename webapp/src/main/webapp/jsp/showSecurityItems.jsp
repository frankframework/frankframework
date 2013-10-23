<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>
<%@ page import="nl.nn.adapterframework.util.RunStateEnum" %>




<page title="Show Security Items" refresh="showSecurityItems.do">

	<xtags:parse>
			<bean:write name="secItems" scope="request" filter="false"/>
	</xtags:parse>

	<xtags:forEach select="securityItems">
		<contentTable>
			<caption>Security Role Bindings</caption>
			<tbody>
				<tr>
					<subHeader>Role</subHeader>
					<subHeader></subHeader>
					<subHeader>SpecialSubjects</subHeader>
					<subHeader>Groups</subHeader>
				</tr>
				<xtags:forEach select="securityRoleBindings/ApplicationBinding/authorizationTable/authorizations">
					<xtags:variable id="count" select="count(groups)"/>
					<xtags:variable id="role" select="substring-after(role/@href,'#')"/>
					<xtags:variable id="srole" select="ancestor::*/applicationDeploymentDescriptor/application/security-role[@id=$role]/role-name"/>
					<tr ref="spannedRow">
						<td rowspan="<%=count%>"><xtags:valueOf select="$srole"/></td>
						<td rowspan="<%=count%>"><booleanImage value="<%=request.isUserInRole(srole)%>"/></td>
						<td rowspan="<%=count%>"><xtags:valueOf select="specialSubjects/@name"/></td>
						<td><xtags:valueOf select="groups[1]/@name"/></td>
					</tr>
					<xtags:remove select="groups[1]"/>
					<xtags:forEach select="groups">
						<tr>
							<td><xtags:valueOf select="@name"/></td>
						</tr>
					</xtags:forEach>
				</xtags:forEach>
			</tbody>
		</contentTable>
		<br/><br/>
		<contentTable>
			<caption>Used JmsRealms</caption>
			<tbody>
				<tr>
					<subHeader>Name</subHeader>
					<subHeader>Datasource</subHeader>
					<subHeader>QueueConnectionFactory</subHeader>
					<subHeader>TopicConnectionFactory</subHeader>
					<subHeader>Info</subHeader>
				</tr>
				<xtags:forEach select="jmsRealms/jmsRealm">
					<tr ref="spannedRow">
						<xtags:variable id="count" select="count(info)"/>
						<td rowspan="<%=count%>"><xtags:valueOf select="@name"/></td>
						<td rowspan="<%=count%>"><xtags:valueOf select="@datasourceName"/></td>
						<td rowspan="<%=count%>"><xtags:valueOf select="@queueConnectionFactoryName"/></td>
						<td rowspan="<%=count%>"><xtags:valueOf select="@topicConnectionFactoryName"/></td>
						<td><xtags:valueOf select="info[1]"/></td>
					</tr>
						<xtags:forEach select="info[position()>1]">
							<tr>
								<td>
									<xtags:valueOf select="."/>
								</td>
							</tr>
						</xtags:forEach>
				</xtags:forEach>
			</tbody>
		</contentTable>
		<br/><br/>
		<contentTable>
			<caption>Used Authentication Entries</caption>
			<tbody>
				<tr>
					<subHeader>Alias</subHeader>
					<subHeader>Username</subHeader>
					<subHeader>Password</subHeader>
				</tr>
				<xtags:forEach select="authEntries/entry">
					<tr ref="spannedRow">
						<td><xtags:valueOf select="@alias"/></td>
						<td><xtags:valueOf select="@userName"/></td>
						<td><xtags:valueOf select="@passWord"/></td>
					</tr>
				</xtags:forEach>
			</tbody>
		</contentTable>
		<br/><br/>
		<contentTable>
			<caption>Used Certificates</caption>
			<tbody>
				<tr>
					<subHeader>Adapter</subHeader>
					<subHeader>Pipe</subHeader>
					<subHeader>Certificate</subHeader>
					<subHeader>Info</subHeader>
				</tr>
				<xtags:forEach select="registeredAdapters/adapter/pipes/pipe/certificate" sort="@url">
					<xtags:variable id="url" select="@url"/>
					<xtags:choose>
						<xtags:when test="preceding::certificate[@url=$url]=true()">
							<tr ref="spannedRow">
								<td><xtags:valueOf select="../../../@name"/></td>
								<td><xtags:valueOf select="../@name"/></td>
								<td><xtags:valueOf select="@name"/></td>
								<td><i>same certificate as above</i></td>
							</tr>
						</xtags:when>
						<xtags:otherwise>
							<xtags:variable id="count" select="count(info)+1"/>
							<tr ref="spannedRow">
								<td rowspan="<%=count%>"><xtags:valueOf select="../../../@name"/></td>
								<td rowspan="<%=count%>"><xtags:valueOf select="../@name"/></td>
								<td rowspan="<%=count%>"><xtags:valueOf select="@name"/></td>
								<td><xtags:valueOf select="@url"/></td>
							</tr>
							<xtags:forEach select="info">
								<tr>
									<td><xtags:valueOf select="."/></td>
								</tr>
							</xtags:forEach>
						</xtags:otherwise>
					</xtags:choose>
				</xtags:forEach>
			</tbody>
		</contentTable>
	</xtags:forEach>
</page>

