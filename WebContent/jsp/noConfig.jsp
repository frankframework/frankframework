<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>

  
<page title="No Configuration">
  <p><b>For some reason, the configuration cannot be retrieved from the ServletContext.</b></p>
  <p>Examine the startup-log and try restarting the server.</p>
  <p>
  <a href="ConfigurationServlet">click here to start the ConfigurationServlet</a><br/>
  The servlet may not output anything, so check the log.
  </p>
</page>		

