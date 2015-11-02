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

import nl.nn.adapterframework.http.HttpUtils;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
/**
 * Handles various function for an adapter: start, stop.
 * @author  Johan Verrips
 * @see nl.nn.adapterframework.core.Adapter
 */

public final class AdapterHandler extends ActionBase {

	public AdapterHandler() {
		setWriteToSecLog(true);
		addSecLogParamName("action");
		addSecLogParamName("adapterName");
		addSecLogParamName("receiverName");
	}
	
public ActionForward executeSub(
    ActionMapping mapping,
    ActionForm form,
    HttpServletRequest request,
    HttpServletResponse response)
    throws IOException, ServletException {
	String adapterName=null;
	String receiverName=null;
    // Initialize action
    initAction(request);

    if (null == ibisManager) {
        return (mapping.findForward("noconfig"));
    }
    String action = request.getParameter("action");
    if (null == action)
        action = mapping.getParameter();
        
    adapterName = request.getParameter("adapterName");
    receiverName = request.getParameter("receiverName");
    log.debug("action ["+action+"] adapterName ["+adapterName+"] receiverName ["+receiverName+"]");
    

    //commandIssuedBy containes information about the location the
    // command is sent from
	String commandIssuedBy= HttpUtils.getCommandIssuedBy(request);
	        
    ibisManager.handleAdapter(action,adapterName,receiverName, commandIssuedBy);
    
    // Report any errors we have discovered back to the original form
    if (!errors.isEmpty()) {
        saveErrors(request, errors);
    } // Forward control to the specified success URI
    log.debug("forward to success");
    return (mapping.findForward("success"));
}
}
