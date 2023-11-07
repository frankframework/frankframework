<%
	// Copied from ../iaf/index.jsp:
	// Do not set 'private', as this indicates it may be cached locally
	// Do not set Expires header to any value, this causes 302 to be cached in Akamai
	response.addHeader("Cache-control","no-cache,no-store,max-age=0,must-revalidate"); 
	response.sendRedirect("../iaf/testtool");
%>