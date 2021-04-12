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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.Dir2Xml;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.webcontrol.FileViewerServlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.w3c.dom.Element;

/**
 * Shows the logging files.
 * <p>Retrieves the values <code>logging.path</code>
 * and <code>logging.wildcard</code> to perform a directory listing
 * that will be shown in the jsp. These values should be stored 
 * in the deployment specific properties file, as the container
 * may override the output of logging.</p>
 * <p>the logging.path variable may be a system variable, e.g.
 * <code><pre>
 * logging.path=${log.dir}
 * <pre></code>
 * </p>
 * Creation date: (26-02-2003 12:42:00)
 * @author Johan Verrips IOS
 */
public class ShowLogging extends ActionBase {
	
	boolean showDirectories = AppConstants.getInstance().getBoolean("logging.showdirectories", false);
	int maxItems = AppConstants.getInstance().getInt("logging.items.max", 500); 
	
	public ActionForward executeSub(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
					throws IOException, ServletException {
	
		// Initialize action
		initAction(request);
		// Retrieve logging directory for browsing
		String path=request.getParameter("directory");
		if (StringUtils.isEmpty(path)) {
			path=AppConstants.getInstance().getResolvedProperty("logging.path");
		}

		boolean sizeFormat = true;
		String sizeFormatString=request.getParameter("sizeFormat");
		if (StringUtils.isNotEmpty(sizeFormatString)) {
			sizeFormat = Boolean.parseBoolean(sizeFormatString);
		}
		
		Dir2Xml dx=new Dir2Xml();
		dx.setPath(path);
		String listresult;
		if (!FileUtils.readAllowed(FileViewerServlet.permissionRules, request, path)) {
				error("access to path ("+path+") not allowed", null);
				listresult="<directory/>";
		} else {
			String wildcard=request.getParameter("wildcard");
			if (StringUtils.isEmpty(wildcard)) {
				wildcard=AppConstants.getInstance().getProperty("logging.wildcard");
			}
			if (wildcard!=null) dx.setWildCard(wildcard);
			try {
				listresult=dx.getDirList(showDirectories, maxItems);
				if (listresult!=null) {
					Element root = XmlUtils.buildDomDocument(listresult).getDocumentElement();
					root.setAttribute("sizeFormat", Boolean.toString(sizeFormat));
					listresult = XmlUtils.nodeToString(root);
					String countStr = root.getAttribute("count");
					if (countStr!=null) {
						int count = Integer.parseInt(countStr);
						if (count > maxItems) {
							error("total number of items ("+count+") exceeded maximum number, only showing first "+maxItems+" items", null);
						}
					}
				}
			} catch (Exception e) {
				error("error occured on getting directory list",e);
				log.warn("returning empty result for directory listing");
				listresult="<directory/>";
			}
		}
		request.setAttribute("Dir2Xml", listresult);
	
		// Report any errors we have discovered back to the original form
		if (!errors.isEmpty()) {
			saveErrors(request, errors);
		}
		// Forward control to the specified success URI
		log.debug("forward to success");
		return (mapping.findForward("success"));
	
	}
}
