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
 * $Log: ShowConfiguration.java,v $
 * Revision 1.16  2011-11-30 13:51:46  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.14  2011/10/05 11:21:04  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * moved code to separate method in ConfigurationUtils
 *
 * Revision 1.13  2011/05/09 14:04:55  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * showConfiguration: added options "show original configuration" and "show loaded configuration"
 *
 * Revision 1.12  2010/05/19 10:31:08  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * show loaded configuration instead of original configuration
 *
 * Revision 1.11  2008/06/03 16:00:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * encode errorMessages
 *
 * Revision 1.10  2007/08/30 15:12:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified getRootLogger()
 *
 * Revision 1.9  2007/02/16 14:22:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed changing of log-level
 *
 * Revision 1.8  2007/02/12 14:35:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.7  2005/12/29 15:35:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made some steps to following links to included configuration files
 *
 * Revision 1.6  2005/10/17 09:37:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added ToDo
 *
 * Revision 1.5  2004/06/16 13:07:41  Johan Verrips <johan.verrips@ibissource.org>
 * Added identity transform functionality to resolve entities
 *
 * Revision 1.4  2004/03/26 10:42:58  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.3  2004/03/23 17:02:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected some typos and solved some warnings
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

/**
 * Shows the configuration (with resolved variables).
 * 
 * @version $Id$
 * @author  Johan Verrips
 * @see nl.nn.adapterframework.configuration.Configuration
 */

public final class ShowConfiguration extends ActionBase {
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
	
	public ActionForward execute(
	    ActionMapping mapping,
	    ActionForm form,
	    HttpServletRequest request,
	    HttpServletResponse response)
	    throws IOException, ServletException {
	
	    // Initialize action
	    initAction(request);
		 
	    DynaActionForm configurationPropertiesForm = getPersistentForm(mapping, form, request);
		configurationPropertiesForm.set("logLevel", LogUtil.getRootLogger().getLevel().toString());
		configurationPropertiesForm.set("logIntermediaryResults", new Boolean(false));
		if (AppConstants.getInstance().getResolvedProperty("log.logIntermediaryResults")!=null) {
			if (AppConstants.getInstance().getResolvedProperty("log.logIntermediaryResults").equalsIgnoreCase("true")) {
				configurationPropertiesForm.set("logIntermediaryResults", new Boolean(true));
			}
		}
	     
	    URL configURL = config.getConfigurationURL();
	    String result = "";
	    try {
			result=ConfigurationUtils.getOriginalConfiguration(configURL);
			if (!AppConstants.getInstance().getBoolean("showConfiguration.original", false)) {
				result=StringResolver.substVars(result, AppConstants.getInstance());
				result = ConfigurationUtils.getActivatedConfiguration(result);
				if (ConfigurationUtils.stubConfiguration()) {
					result = ConfigurationUtils.getStubbedConfiguration(result);
				}			
			}			
		} catch (ConfigurationException e) {
			result =
				"<b>error occured retrieving configurationfile:" + XmlUtils.encodeChars(e.getMessage()) + "</b>";
	    }
			
	    request.setAttribute("configXML", result);
	
	    // Forward control to the specified success URI
	    log.debug("forward to success");
	    return (mapping.findForward("success"));
	
	}
}
