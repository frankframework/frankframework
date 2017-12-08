<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>

<page title="IBIS startup failed">
	<p><b>For some reason, the IBIS application failed to start up.</b></p>
	<p style="margin-top: 10px;">Please examine the startup-log and try restarting the server.</p>
	<p style="color: red; margin-top: 80px;"><b><i>NOTE: The IBIS application will automatically retry to startup every minute.</i></b></p>
	<p><a href="ConfigurationServlet">Click here to manually retry to start the IBIS application.</a></p>

<script type="text/javascript">
	setTimeout(function () { window.location.reload(); }, 60000);
</script>
</page>