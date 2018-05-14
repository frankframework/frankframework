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
						<td rowspan="<%=(count=="0"?"1":count)%>"><xtags:valueOf select="$srole"/></td>
						<td rowspan="<%=(count=="0"?"1":count)%>"><booleanImage value="<%=request.isUserInRole(srole)%>"/></td>
						<td rowspan="<%=(count=="0"?"1":count)%>"><xtags:valueOf select="specialSubjects/@name"/></td>
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
			<caption>Provided JmsDestinations</caption>
			<tbody>
				<tr>
					<subHeader>ConnectionFactory</subHeader>
					<subHeader>Name</subHeader>
					<subHeader>Value</subHeader>
					<subHeader>Used</subHeader>
				</tr>
				<xtags:choose>
					<xtags:when test="providedJmsDestinations/@error='true'">
						<tr>
							<td colspan="4"><font color="red"><xtags:valueOf select="concat('ERROR: ', providedJmsDestinations)"/></font></td>
						</tr>				
					</xtags:when>
					<xtags:when test="providedJmsDestinations/@warn='true'">
						<tr>
							<td colspan="4"><font color="orange"><xtags:valueOf select="concat('WARN: ', providedJmsDestinations)"/></font></td>
						</tr>				
					</xtags:when>
					<xtags:otherwise>
						<xtags:forEach select="providedJmsDestinations/connectionFactory">
							<xtags:variable id="cf" select="@jndiName"/>
							<tr ref="spannedRow">
								<xtags:variable id="count" select="count(destination)"/>
								<td rowspan="<%=(count=="0"?"1":count)%>"><xtags:valueOf select="$cf"/></td>
								<td><xtags:valueOf select="destination[1]/@jndiName"/></td>
								<td><xtags:valueOf select="destination[1]"/></td>
								<td>
									<xtags:variable id="used" select="destination[1]/@used"/>
									<booleanImage value="<%=used%>"/>
								</td>
							</tr>
							<xtags:forEach select="destination[position()>1]">
								<tr>
									<td><xtags:valueOf select="@jndiName"/></td>
									<td><xtags:valueOf select="."/></td>
								<td>
									<xtags:variable id="used" select="@used"/>
									<booleanImage value="<%=used%>"/>
								</td>
								</tr>
							</xtags:forEach>
						</xtags:forEach>
					</xtags:otherwise>
				</xtags:choose>
			</tbody>
		</contentTable>
		<br/><br/>
		<contentTable>
			<caption>Used SapSystems</caption>
			<tbody>
				<tr>
					<subHeader>Name</subHeader>
					<subHeader>Info</subHeader>
				</tr>
				<xtags:forEach select="sapSystems/sapSystem">
					<tr ref="spannedRow">
						<td><xtags:valueOf select="@name"/></td>
						<td><xtags:valueOf select="info"/></td>
					</tr>
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
		<br/><br/>
		<contentTable>
			<caption>Transaction Service</caption>
			<tbody>
				<tr>
					<subHeader>Property</subHeader>
					<subHeader>Value</subHeader>
				</tr>
				<xtags:forEach select="serverProps/transactionService">
					<tr ref="spannedRow">
						<td>Total transaction lifetime timeout (in seconds)</td>
						<td><xtags:valueOf select="@totalTransactionLifetimeTimeout"/></td>
					</tr>
					<tr ref="spannedRow">
						<td>Maximum transaction timeout (in seconds)</td>
						<td><xtags:valueOf select="@maximumTransactionTimeout"/></td>
					</tr>
				</xtags:forEach>
			</tbody>
		</contentTable>
	</xtags:forEach>
</page>

