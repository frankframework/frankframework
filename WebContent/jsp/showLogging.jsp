<%@ page import="nl.nn.adapterframework.util.DateUtils"%>
<%@ page import="java.util.Date"%>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>

  
<page title="Adapter logging">
  
	<contentTable>
		<caption>Logfiles</caption>
		<tbody>
			<jsp:include page="/jsp/directoryLister.jsp" flush="true"/>
		</tbody>
	</contentTable>
</page>		

