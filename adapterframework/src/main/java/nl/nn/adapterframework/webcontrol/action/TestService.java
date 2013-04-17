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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.receivers.ServiceDispatcher;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

/**
 * Testing a service prefill.
 * 
 * @author Johan Verrips
 * @version $Id$
 */
public class TestService extends ActionBase {
	public static final String version = "$RCSfile: TestService.java,v $ $Revision: 1.6 $ $Date: 2011-11-30 13:51:46 $";
	
	public ActionForward execute(
	    ActionMapping mapping,
	    ActionForm form,
	    HttpServletRequest request,
	    HttpServletResponse response)
	    throws IOException, ServletException {
	
	    // Initialize action
	    initAction(request);
	    if (null == config) {
	        return (mapping.findForward("noconfig"));
	    }
	    DynaActionForm serviceTestForm = getPersistentForm(mapping, form, request);
	
		Iterator it=ServiceDispatcher.getInstance().getRegisteredListenerNames();
		List services=new ArrayList();
		services.add("----- select a service -----");
		while (it.hasNext()) {
			services.add((String)it.next());
		}
		serviceTestForm.set("services", services);
		// Forward control to the specified success URI
	    log.debug("forward to success");
	    return (mapping.findForward("success"));
	    
	}
}
