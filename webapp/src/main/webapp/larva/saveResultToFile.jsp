<%@page import="nl.nn.adapterframework.testtool.TestTool"
%><%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
%><% request.setCharacterEncoding("UTF-8");
%><?xml version="1.0" encoding="UTF-8"?>
<!--force IE7 into quirks mode-->
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		<title><%
			if(request.getParameter("cmd") != null) {
				if (request.getParameter("cmd").equals("indentWindiff")) {
					out.print("Comparing result");
				} else {
					out.print("Save actual result");
				}
			}
			%></title>
		<link rel="stylesheet" type="text/css" href="assets/style.css"/>
		<script type="text/javascript" src="assets/lib.js"></script>
	</head>
	<body>
		<%
		if (request.getParameter("init") != null) {
			out.print("<p>Waiting for data...</p>");
		} else if (request.getParameter("expectedFileName") == null) {
			out.print("<p>No file name received!</p>");
			out.print("<p>In case you use Tomcat and large messages this might be caused by maxPostSize which is set to 2097152 (2 megabytes) by default. Add maxPostSize to the Connector element in server.xml with a larger value or 0.</p>");
		} else {
			if (request.getParameter("cmd").equals("indentWindiff")) {
				out.print("<p>Comparing actual result with expected result...</p>");
				out.flush();
				TestTool.windiff(request.getParameter("expectedFileName"), request.getParameter("resultBox"), request.getParameter("expectedBox"));
			} else {
				out.print("<p>Overwriting expected result with actual result...</p>");
				out.flush();
				TestTool.writeFile(request.getParameter("expectedFileName"), request.getParameter("resultBox"));
			}
			out.print("<p>Done!</p>");
			out.print("<script>window.close();</script>");
		}
		%>
	<p><a href="javascript:window.close()">close</a></p>
	</body>
</html>
