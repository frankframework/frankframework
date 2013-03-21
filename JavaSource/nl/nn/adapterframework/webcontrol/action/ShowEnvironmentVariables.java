/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
/*
 * $Log: ShowEnvironmentVariables.java,v $
 * Revision 1.7  2011-11-30 13:51:46  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.5  2011/06/20 13:27:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Java 5.0 compatibility
 *
 * Revision 1.4  2006/08/22 06:56:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused imports
 *
 * Revision 1.3  2005/12/28 09:18:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support unicode names for environment-variables
 *
 * Revision 1.2  2005/10/27 08:44:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
 * @version $Id$ 
 * @since	4.4
 */

public class ShowEnvironmentVariables extends ActionBase {

	public void addPropertiesToXmlBuilder(XmlBuilder container, Properties props, String setName) {
		Enumeration enumeration = props.keys();
		XmlBuilder propertySet = new XmlBuilder("propertySet");
		propertySet.addAttribute("name", setName);
		container.addSubElement(propertySet);

		while (enumeration.hasMoreElements()) {
			String propName = (String) enumeration.nextElement();
			XmlBuilder property = new XmlBuilder("property");
			property.addAttribute("name", XmlUtils.encodeCdataString(propName));
			property.setCdataValue(XmlUtils.encodeCdataString(props.getProperty(propName)));
			propertySet.addSubElement(property);
		}

	}
	

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
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
