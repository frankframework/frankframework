<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%><%
	request.setCharacterEncoding("UTF-8");
%>
<?xml version="1.0" encoding="UTF-8"?>
<!--force IE7 into quirks mode-->
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		<title><%
			if (request.getParameter("cmd").equals("indentWindiff")) {
				out.print("Comparing result");
			} else {
				out.print("Save actual result");
			}
			%></title>
		<link rel="stylesheet" type="text/css" href="assets/style.css"/>
		<script type="text/javascript" src="assets/lib.js"></script>
		<%@ page import="nl.nn.adapterframework.testtool.TestTool"%>
		<%@ page import="java.io.IOException"%>
	</head>
	<body>
		<p><%
			if (request.getParameter("cmd").equals("indentWindiff")) {
				out.print("Comparing actual result with expected result...");
			} else {
				out.print("Overwriting expected result with actual result...");
			}
			%></p>
<%
	try {
		if (request.getParameter("expectedFileName") == null) {
			// IE8 seems to call this page twice, ignore first time without
			// expectedFileName parameter.
			out.print("<p>No file name received!</p>");
		} else {
			if (request.getParameter("cmd").equals("indentWindiff")) {
				TestTool.windiff(request.getParameter("expectedFileName"), request.getParameter("resultBox"), request.getParameter("expectedBox"));
			} else {
				TestTool.writeFile(request.getParameter("expectedFileName"), request.getParameter("resultBox"));
			}
			out.print("<p>Done!</p>");
		}
	} catch (IOException e) {
		out.print("<p>IOException: " + e.getMessage() + "</p>");
	}
%>
	<p><a href="javascript:window.close()">close</a></p>
	</body>
</html>