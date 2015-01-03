<%@ page
	language="java"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
	import = "nl.nn.adapterframework.testtool.TestTool"
%>
<%--
	Author Jaco de Groot
--%>

<?xml version="1.0" encoding="UTF-8"?>
<!--force IE7 into quirks mode-->
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

	<head>
		<title>Test Tool</title>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		<link rel="stylesheet" type="text/css" href="assets/style.css"/>
		<script type="text/javascript" src="assets/lib.js"></script>
	</head>

	<body>

<%
TestTool.runScenarios(application, request, out);
%>

	</body>
</html>
