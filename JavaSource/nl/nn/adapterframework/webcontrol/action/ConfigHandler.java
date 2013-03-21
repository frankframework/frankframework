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
 * $Log: ConfigHandler.java,v $
 * Revision 1.3  2011-11-30 13:51:46  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2011/05/09 14:04:55  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * showConfiguration: added options "show original configuration" and "show loaded configuration"
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.util.AppConstants;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This handler adjusts the way the configuration is shown (original or loaded).
 * @version $Id$
 * @author  Peter Leeuwenburgh
 */

public final class ConfigHandler extends ActionBase {

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		String adapterName = null;
		String receiverName = null;
		// Initialize action
		initAction(request);

		if (null == ibisManager) {
			return (mapping.findForward("noconfig"));
		}
		String action = request.getParameter("action");
		if (null == action)
			action = mapping.getParameter();

		if (action.equals("showoriginalconfig")) {
			AppConstants.getInstance().put("showConfiguration.original", "true");
		} else {
			AppConstants.getInstance().put("showConfiguration.original", "false");
		}
		
		// Report any errors we have discovered back to the original form
		if (!errors.isEmpty()) {
			saveErrors(request, errors);
		} // Forward control to the specified success URI
		log.debug("forward to success");
		return (mapping.findForward("success"));
	}
}
