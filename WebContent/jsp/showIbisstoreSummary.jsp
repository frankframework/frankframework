
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>


<page title="Show Ibisstore Summary">
	<logic:notEmpty name="result" scope="request">
		<xtags:parse>
			<bean:write name="result" scope="request" filter="false"/>
		</xtags:parse>
	</logic:notEmpty>
   
  	<xtags:stylesheet >
	  	<xtags:template match="/">

<html:xhtml/>
<html:form action="/showIbisstoreSummary.do">
<html:hidden property="action"/>

<table border="0" width="100%">
<tr>
	<td>
		Select a jms realm
	</td>
	<td>
	
		<html:select property="jmsRealm">	
			<html:options name="executeJdbcQueryForm" property="jmsRealms"/> 
		</html:select> <br/>
  	 	
	</td>
   </tr>

  <tr>
    <td>
      <html:cancel>cancel</html:cancel>
    </td>
    <td>
      <html:submit>send</html:submit>
    </td>
  </tr>

</table>
</html:form>

	<contentTable>
		<caption>Ibisstore Summary</caption>
		<tbody>
			<tr>
				<th>Type</th>
				<th>#</th>
				<th>SlotID</th>
				<th>#</th>
				<th>Date</th>
				<th>#</th>
			</tr>

<!--
 	<xtags:forEach select="//rowset/row">
 		<tr>
 			<td><xtags:valueOf select="field[@name='TYPE']"/></td>
 			<td><xtags:valueOf select="field[@name='SLOTID']"/></td>
 			<td><xtags:valueOf select="field[@name='MSGDATE']"/></td>
 			<td><xtags:valueOf select="field[@name='MSGCOUNT']"/></td>
 		</tr>
 	</xtags:forEach>
--> 	
 	<xtags:forEach select="//type">
 		<tr>
 			<td rowspan="<xtags:valueOf select="@datecount"/>">
 				<xtags:valueOf select="@id"/>
 			</td>
 			<td rowspan="<xtags:valueOf select="@datecount"/>">
				<xtags:valueOf select="@msgcount"/>
 			</td>
 			<td rowspan="<xtags:valueOf select="slot[1]/@datecount"/>">
 				<xtags:valueOf select="slot[1]/@id"/>
 				<xtags:if test="slot[1]/@adapter!=''">
 					<xtags:variable id="type" select="@id"/>
					<imagelink
						href="browser.do"
						type="browse"
						<%    if ( "E".equalsIgnoreCase(type) ) { %>
							alt="show contents of errorQueue"
						<% } else { %>
							alt="show contents of messageLog"
						<%}%>
						>
						<parameter name="storageType"><xtags:valueOf select="@name"/></parameter>
						<parameter name="action">show</parameter>
						<parameter name="adapterName"><xtags:valueOf select="slot[1]/@adapter"/></parameter>
						<parameter name="receiverName"><xtags:valueOf select="slot[1]/@receiver"/></parameter>
						<parameter name="pipeName"><xtags:valueOf select="slot[1]/@pipe"/></parameter>
						<parameter name="typeMask"><xtags:valueOf select="@id"/></parameter>
					 </imagelink>
					(<xtags:valueOf select="slot[1]/@adapter"/> / <xtags:valueOf select="slot[1]/@receiver"/>)
				</xtags:if>
 			</td>
 			<td rowspan="<xtags:valueOf select="slot[1]/@datecount"/>">
				<xtags:valueOf select="slot[1]/@msgcount"/>
 			</td>
 			<td>
 				<xtags:valueOf select="slot[1]/date[1]/@id"/>
 				<xtags:if test="slot[1]/@adapter!=''">
 					<xtags:variable id="type" select="@id"/>
					<imagelink
						href="browser.do"
						type="browse"
						<%    if ( "E".equalsIgnoreCase(type) ) { %>
							alt="show contents of errorQueue"
						<% } else { %>
							alt="show contents of messageLog"
						<%}%>
						>
						<parameter name="storageType"><xtags:valueOf select="@name"/></parameter>
						<parameter name="action">show</parameter>
						<parameter name="adapterName"><xtags:valueOf select="slot[1]/@adapter"/></parameter>
						<parameter name="receiverName"><xtags:valueOf select="slot[1]/@receiver"/></parameter>
						<parameter name="pipeName"><xtags:valueOf select="slot[1]/@pipe"/></parameter>
						<parameter name="typeMask"><xtags:valueOf select="@id"/></parameter>
						<parameter name="insertedAfter"><xtags:valueOf select="slot[1]/date[1]/@id"/></parameter>
						<parameter name="insertedAfterClip">on</parameter>
					 </imagelink>
				</xtags:if>
 			</td>
 			<td>
				<xtags:valueOf select="slot[1]/date[1]/@count"/>
 			</td>
 		</tr>
		<xtags:forEach select="slot[1]/date[position()>1]" >
			<tr>
	 			<td>
	 				<xtags:valueOf select="@id"/>
	 				<xtags:if test="../@adapter!=''">
	 					<xtags:variable id="type" select="../../@id"/>
						<imagelink
							href="browser.do"
							type="browse"
							<%    if ( "E".equalsIgnoreCase(type) ) { %>
								alt="show contents of errorQueue"
							<% } else { %>
								alt="show contents of messageLog"
							<%}%>
							>
							<parameter name="storageType"><xtags:valueOf select="../../@name"/></parameter>
							<parameter name="action">show</parameter>
							<parameter name="adapterName"><xtags:valueOf select="../@adapter"/></parameter>
							<parameter name="receiverName"><xtags:valueOf select="../@receiver"/></parameter>
							<parameter name="pipeName"><xtags:valueOf select="../@pipe"/></parameter>
							<parameter name="typeMask"><xtags:valueOf select="../../@id"/></parameter>
							<parameter name="insertedAfter"><xtags:valueOf select="@id"/></parameter>
							<parameter name="insertedAfterClip">on</parameter>
						 </imagelink>
					</xtags:if>
	 			</td>
 				<td>
					<xtags:valueOf select="@count"/>
	 			</td>
	 		</tr>
 		</xtags:forEach>
 		
		<xtags:forEach select="slot[position()>1]" >
	 		<tr>
	 			<td rowspan="><xtags:valueOf select="@datecount"/>">
	 				<xtags:valueOf select="@id"/>
	 				<xtags:if test="@adapter!=''">
	 					<xtags:variable id="type" select="../@id"/>
						<imagelink
							href="browser.do"
							type="browse"
							<%    if ( "E".equalsIgnoreCase(type) ) { %>
								alt="show contents of errorQueue"
							<% } else { %>
								alt="show contents of messageLog"
							<%}%>
							>
							<parameter name="storageType"><xtags:valueOf select="../@name"/></parameter>
							<parameter name="action">show</parameter>
							<parameter name="adapterName"><xtags:valueOf select="@adapter"/></parameter>
							<parameter name="receiverName"><xtags:valueOf select="@receiver"/></parameter>
							<parameter name="pipeName"><xtags:valueOf select="@pipe"/></parameter>
							<parameter name="typeMask"><xtags:valueOf select="../@id"/></parameter>
						 </imagelink>
						(<xtags:valueOf select="@adapter"/> / <xtags:valueOf select="@receiver"/>)
	 				</xtags:if>
	 			</td>
	 			<td rowspan="><xtags:valueOf select="@datecount"/>">
					<xtags:valueOf select="@msgcount"/>
		 		</td>
	 			<td>
	 				<xtags:valueOf select="date[1]/@id"/>
	 				<xtags:if test="@adapter!=''">
	 					<xtags:variable id="type" select="../@id"/>
						<imagelink
							href="browser.do"
							type="browse"
							<%    if ( "E".equalsIgnoreCase(type) ) { %>
								alt="show contents of errorQueue"
							<% } else { %>
								alt="show contents of messageLog"
							<%}%>
							>
							<parameter name="storageType"><xtags:valueOf select="../@name"/></parameter>
							<parameter name="action">show</parameter>
							<parameter name="adapterName"><xtags:valueOf select="@adapter"/></parameter>
							<parameter name="receiverName"><xtags:valueOf select="@receiver"/></parameter>
							<parameter name="pipeName"><xtags:valueOf select="@pipe"/></parameter>
							<parameter name="typeMask"><xtags:valueOf select="../@id"/></parameter>
							<parameter name="insertedAfter"><xtags:valueOf select="date[1]/@id"/></parameter>
							<parameter name="insertedAfterClip">on</parameter>
						 </imagelink>
					</xtags:if>
	 			</td>
 				<td>
					<xtags:valueOf select="date[1]/@count"/>
	 			</td>
	 		</tr>
			<xtags:forEach select="date[position()>1]" >
		 		<tr>
		 			<td>
		 				<xtags:valueOf select="@id"/>
		 				<xtags:if test="../@adapter!=''">
		 					<xtags:variable id="type" select="../../@id"/>
							<imagelink
								href="browser.do"
								type="browse"
								<%    if ( "E".equalsIgnoreCase(type) ) { %>
									alt="show contents of errorQueue"
								<% } else { %>
									alt="show contents of messageLog"
								<%}%>
								>
								<parameter name="storageType"><xtags:valueOf select="../../@name"/></parameter>
								<parameter name="action">show</parameter>
								<parameter name="adapterName"><xtags:valueOf select="../@adapter"/></parameter>
								<parameter name="receiverName"><xtags:valueOf select="../@receiver"/></parameter>
								<parameter name="pipeName"><xtags:valueOf select="../@pipe"/></parameter>
								<parameter name="typeMask"><xtags:valueOf select="../../@id"/></parameter>
								<parameter name="insertedAfter"><xtags:valueOf select="@id"/></parameter>
								<parameter name="insertedAfterClip">on</parameter>
							 </imagelink>
						</xtags:if>
	 				</td>
	 				<td>
						<xtags:valueOf select="@count"/>
		 			</td>
		 		</tr>
		 	</xtags:forEach>
	 	</xtags:forEach>
 	</xtags:forEach>
		</tbody>
		</contentTable>
		
		</xtags:template>

 	</xtags:stylesheet>


</page>

