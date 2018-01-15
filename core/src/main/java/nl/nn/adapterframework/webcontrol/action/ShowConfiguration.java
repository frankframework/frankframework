/*
   Copyright 2013, 2016, 2018 Nationale-Nederlanden

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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.extensions.log4j.IbisAppenderWrapper;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

/**
 * Shows the configuration (with resolved variables).
 * 
 * @author  Johan Verrips
 * @see nl.nn.adapterframework.configuration.Configuration
 */

public final class ShowConfiguration extends ActionBase {
	private static final String CONFIG_ALL = "*ALL*";
	private static final String KEYWORD_INCLUDE="<include";
	private static final String KEYWORD_CONFIG="configuration=\"";
	private static final String KEYWORD_QUOTE="\"";
	
	private void checkForInclude(String line, List linklist) {
		if (line==null) {
			return;
		}
		int includePos=line.indexOf(KEYWORD_INCLUDE);
		if (includePos<0) {
			return;
		}
		int configurationStartPos=line.indexOf(KEYWORD_CONFIG,includePos+KEYWORD_INCLUDE.length());
		if (configurationStartPos<0) {
			return;
		}
		configurationStartPos+=KEYWORD_CONFIG.length();
		int configurationEndPos=line.indexOf(KEYWORD_QUOTE,configurationStartPos);

		String configuration=line.substring(configurationStartPos,configurationEndPos);	
		log.debug("configuration found ["+configuration+"]");
		linklist.add(configuration);
	}
	
	public ActionForward executeSub(
	    ActionMapping mapping,
	    ActionForm form,
	    HttpServletRequest request,
	    HttpServletResponse response)
	    throws IOException, ServletException {
	
	    // Initialize action
	    initAction(request);
		if(ibisManager==null)return (mapping.findForward("noIbisContext"));
		 
		XmlBuilder configurationsXml = new XmlBuilder("configurations");
		List<Configuration> configurations = ibisManager.getConfigurations();
		XmlBuilder configurationAllXml = new XmlBuilder("configuration");
		configurationAllXml.setValue(CONFIG_ALL);
		configurationAllXml.addAttribute("nameUC","0" + Misc.toSortName(CONFIG_ALL));
		configurationsXml.addSubElement(configurationAllXml);
		for (Configuration configuration : configurations) {
			XmlBuilder configurationXml = new XmlBuilder("configuration");
			configurationXml.setValue(configuration.getConfigurationName());
			configurationXml.addAttribute("nameUC","1" + Misc.toSortName(configuration.getConfigurationName()));
			configurationsXml.addSubElement(configurationXml);
		}
		request.setAttribute("configurations", configurationsXml.toXML());

		String result = "";
		String configurationName = request.getParameter("configuration");
		if (configurationName == null) {
			configurationName = (String)request.getSession().getAttribute("configurationName");
		}
		if (configurationName == null
				|| configurationName.equalsIgnoreCase(CONFIG_ALL)
				|| ibisManager.getConfiguration(configurationName) == null) {
			for (Configuration configuration : ibisManager.getConfigurations()) {
				if (AppConstants.getInstance().getBoolean("showConfiguration.original", false)) {
					result = result + configuration.getOriginalConfiguration();
				} else {
					result = result + configuration.getLoadedConfiguration();
				}
			}
			request.getSession().setAttribute("configurationName", CONFIG_ALL);
		} else {
			Configuration configuration = ibisManager.getConfiguration(configurationName);
			if (AppConstants.getInstance().getBoolean("showConfiguration.original", false)) {
				result = configuration.getOriginalConfiguration();
			} else {
				result = configuration.getLoadedConfiguration();
			}
			request.getSession().setAttribute("configurationName", configuration.getConfigurationName());
		}

	    request.setAttribute("configXML", result);

	    // Forward control to the specified success URI
	    log.debug("forward to success");
	    return (mapping.findForward("success"));
	
	}
}
