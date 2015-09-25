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
package nl.nn.adapterframework.webcontrol.action;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Shows the environment variables.
 * 
 * @author  Peter Leeuwenburgh 
 * @since	4.4
 */

public class ShowEnvironmentVariables extends ActionBase {

	public void addPropertiesToXmlBuilder(XmlBuilder container, Properties props, String setName, List<String> propsToHide) {
		Enumeration enumeration = props.keys();
		XmlBuilder propertySet = new XmlBuilder("propertySet");
		propertySet.addAttribute("name", setName);
		container.addSubElement(propertySet);

		while (enumeration.hasMoreElements()) {
			String propName = (String) enumeration.nextElement();
			XmlBuilder property = new XmlBuilder("property");
			property.addAttribute("name", XmlUtils.encodeCdataString(propName));
			String propValue = props.getProperty(propName);
        	if (propsToHide != null && propsToHide.contains(propName)) {
        		propValue = Misc.hide(propValue);
        	}
			property.setCdataValue(XmlUtils.encodeCdataString(propValue));
			propertySet.addSubElement(property);
		}

	}

	public void addPropertiesToXmlBuilder(XmlBuilder container, Properties props, String setName) {
		addPropertiesToXmlBuilder(container, props, setName, null);
	}

	public ActionForward executeSub(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		// Initialize action
		initAction(request);
		// Retrieve environment variables for browsing

		List<String> propsToHide = new ArrayList<String>();
		String propertiesHideString = AppConstants.getInstance().getString("properties.hide", null);
		if (propertiesHideString!=null) {
			propsToHide.addAll(Arrays.asList(propertiesHideString.split("[,\\s]+")));
		}
		
		XmlBuilder envVars = new XmlBuilder("environmentVariables");

		addPropertiesToXmlBuilder(envVars,AppConstants.getInstance(),"Application Constants",propsToHide);
		addPropertiesToXmlBuilder(envVars,System.getProperties(),"System Properties",propsToHide);
		
		try {
			addPropertiesToXmlBuilder(envVars,Misc.getEnvironmentVariables(),"Environment Variables");
		} catch (Throwable t) {
			log.warn("caught Throwable while getting EnvironmentVariables",t);
		}
	
		request.setAttribute("envVars", envVars.toXML());

		// Forward control to the specified success URI
		log.debug("forward to success");
		return (mapping.findForward("success"));
	}
	
}
