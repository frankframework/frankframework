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
 * $Log: AdapterHandler.java,v $
 * Revision 1.6  2011-11-30 13:51:46  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2007/10/10 07:30:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * execute control via IbisManager
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
/**
 * Handles various function for an adapter: start, stop.
 * @version $Id$
 * @author  Johan Verrips
 * @see nl.nn.adapterframework.core.Adapter
 */

public final class AdapterHandler extends ActionBase {
	public static final String version="$RCSfile: AdapterHandler.java,v $ $Revision: 1.6 $ $Date: 2011-11-30 13:51:46 $";



public ActionForward execute(
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
	String commandIssuedBy= getCommandIssuedBy(request);
	        
    ibisManager.handleAdapter(action,adapterName,receiverName, commandIssuedBy);
    
    // Report any errors we have discovered back to the original form
    if (!errors.isEmpty()) {
        saveErrors(request, errors);
    } // Forward control to the specified success URI
    log.debug("forward to success");
    return (mapping.findForward("success"));
}
}
