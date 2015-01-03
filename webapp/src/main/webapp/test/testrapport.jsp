<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%@ page import="nl.nn.adapterframework.testtool.TestRapportRegel" %>
<%@ page import="nl.nn.adapterframework.testtool.TestRapport" %>
<%@ page import="nl.nn.adapterframework.testtool.TestRapportFoRegel" %>
<%@ page import="nl.nn.adapterframework.testtool.TestRapportStepRegel" %>

<HTML>
<HEAD>
	<META http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
 	<META name="GENERATOR" content="IBM WebSphere Studio">
 	<TITLE>Test rapport</TITLE>
</HEAD>
<% 
	TestRapport testRapport = null;
	TestRapportRegel testRapportRegel = null;
		
	if (session.getAttribute("testRapport") != null) {
 	 	testRapport = (TestRapport)session.getAttribute("testRapport");
 	}
%>


<BODY>
	<H1>Test rapport</H1>
	Datum: <%= testRapport.getDatum() %><br/>
	Applicatie: <%= testRapport.getInstantie() %>
	
	<H2>Samenvatting</H2>
	<TABLE border="1">
		<TR>
			<TH>Functionaliteit</TH>
			<TH>Korte omschrijving</TH>
			<TH>Resultaat</TH>
		</TR>
		<%
			for (int i = 0; i < testRapport.size(); i++) {
				testRapportRegel = testRapport.getTestRapportRegel(i);
		%>
			<TR>
				<TD><%= testRapportRegel.getGebied() %> </TD>
				<TD><%= testRapportRegel.getDescription() %> </TD>
				<TD><%= testRapportRegel.getStatus() %></TD>
			</TR>	
		<%
			}
		%>	
	</TABLE>
	<H2>Details</H2>
	<%
		for (int i = 0; i < testRapport.size(); i++) {
			testRapportRegel = testRapport.getTestRapportRegel(i);
	%>
		<HR/>
		<BR/>
		<b>Functionaliteit</b><br/>
		<%= testRapportRegel.getGebied() %><br/>
		<br/>
		<b>Korte omschrijving</b><br/>
		<%= testRapportRegel.getDescription() %><br/>
		<br/>
		<b>Volledige beschrijving</b><br/>
		<%= testRapportRegel.getFullDescriptionHTML() %><br/> 
		<br/>
		<b>FO gegevens</b><br/>
		<br/>
		<table border="1">
			<TR>
				<TH>Naam FO</TH>
				<TH>Versie FO</TH>
			</TR>
			<%
				for (int n = 0; n < testRapportRegel.foListSize(); n++) {
					TestRapportFoRegel testRapportFoRegel = testRapportRegel.getTestRapportFoRegel(n);				
			%>
				<TR>
					<TD><%= testRapportFoRegel.getName() %></TD>
					<TD><%= testRapportFoRegel.getVersion() %></TD>
				</TR>
			<% } %>
		</table>
		<br/>
		<b>Stappen</b><br/>
		<br/>
		<table border="1">
			<TR>
				<TH>Naam stap</TH>
				<TH>Type</TH>
				<TH>Richting</TH>
				<TH>Doel</TH>
				<TH>XML file (input / verwacht)</TH> 
			</TR>
			<%
				for (int n = 0; n < testRapportRegel.stepListSize(); n++) {
					TestRapportStepRegel testRapportStepRegel = testRapportRegel.getTestRapportStepRegel(n);				
			%>
				<TR>
					<TD><%= testRapportStepRegel.getName() %></TD>
					<TD><%= testRapportStepRegel.getType() %></TD>
					<TD><%= testRapportStepRegel.getRichting() %></TD>
					<TD><%= testRapportStepRegel.getDoel() %></TD>
					<TD>
						<a href="<%= testRapportRegel.getDirectory() + testRapportStepRegel.getFile() %>">
							<%= testRapportStepRegel.getFile() %>	
						</a>
					</TD>
				</TR>
			<% } %>
		</table>
		
	<% } %>	

	
</BODY>
</HTML>
