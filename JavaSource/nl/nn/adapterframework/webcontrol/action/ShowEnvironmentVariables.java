/*
 * $Log: ShowEnvironmentVariables.java,v $
 * Revision 1.2  2005-10-27 08:44:07  europe\L190409
 * moved getEnvironmentVariables to Misc
 *
 * Revision 1.1  2005/10/26 12:52:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added ShowEnvironmentVariables to console
 *
 */

package nl.nn.adapterframework.webcontrol.action;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Properties;

import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Shows the environment variables.
 * 
 * @author  Peter Leeuwenburgh
 * @version Id 
 * @since	4.4
 */

public class ShowEnvironmentVariables extends ActionBase {
	public static final String version = "$RCSfile: ShowEnvironmentVariables.java,v $ $Revision: 1.2 $ $Date: 2005-10-27 08:44:07 $";

	public void addPropertiesToXmlBuilder(XmlBuilder container, Properties props, String setName) {
		Enumeration enum = props.keys();
		XmlBuilder propertySet = new XmlBuilder("propertySet");
		propertySet.addAttribute("name", setName);
		container.addSubElement(propertySet);

		while (enum.hasMoreElements()) {
			String propName = (String) enum.nextElement();
			XmlBuilder property = new XmlBuilder("property");
			property.addAttribute("name", propName);
			property.setCdataValue(
				XmlUtils.encodeCdataString(props.getProperty(propName)));
			propertySet.addSubElement(property);
		}

	}
	

	public ActionForward execute(
		ActionMapping mapping,
		ActionForm form,
		HttpServletRequest request,
		HttpServletResponse response)
		throws IOException, ServletException {
		// Initialize action
		initAction(request);
		// Retrieve environment variables for browsing

		XmlBuilder envVars = new XmlBuilder("environmentVariables");

		addPropertiesToXmlBuilder(envVars,AppConstants.getInstance(),"Application Constants");
		addPropertiesToXmlBuilder(envVars,System.getProperties(),"System Properties");
		
		try {
			addPropertiesToXmlBuilder(envVars,Misc.getEnvironmentVariables(),"Environment Variables");
		} catch (Throwable t) {
			log.warn("caught Throwable while getting EnvironmentVariables",t);
		}
		
		if (log.isDebugEnabled()) {
			log.debug("envVars: [" + envVars.toXML() + "]");
		}
		request.setAttribute("envVars", envVars.toXML());

		// Forward control to the specified success URI
		log.debug("forward to success");
		return (mapping.findForward("success"));

	}
}
