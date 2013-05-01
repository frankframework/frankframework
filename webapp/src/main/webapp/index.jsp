<%@ page language="java" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/xtags.tld" prefix="xtags" %>
<html:base/> 

<%-- Just a page to forward to the configurationStatus view --%>

 	<%pageContext.forward("/showConfigurationStatus.do");%>
 