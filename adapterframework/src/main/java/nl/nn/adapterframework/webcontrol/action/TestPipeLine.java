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
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.RunStateEnum;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;


/**
 * Test the Pipeline of an adapter.
 * 
 * @author  Johan Verrips
 * @version $Id$
 * @see nl.nn.adapterframework.configuration.Configuration
 * @see nl.nn.adapterframework.core.Adapter
 * @see nl.nn.adapterframework.core.PipeLine
 */
public final class TestPipeLine extends ActionBase {
	public static final String version="$RCSfile: TestPipeLine.java,v $ $Revision: 1.8 $ $Date: 2011-11-30 13:51:46 $";

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
	
	    // Initialize action
	    initAction(request);
	    if (null == config) {
	        return (mapping.findForward("noconfig"));
	    }
	
	    DynaActionForm pipeLineTestForm = getPersistentForm(mapping, form, request);
	
	    List startedAdapters = new ArrayList();
	
		for(int i=0; i<config.getRegisteredAdapters().size(); i++) {
			IAdapter adapter = config.getRegisteredAdapter(i);
	        // add the adapter if it is started.
	        if (adapter.getRunState().equals(RunStateEnum.STARTED)) {
		        startedAdapters.add(adapter.getName());
	        }
	    }
		Collections.sort(startedAdapters, String.CASE_INSENSITIVE_ORDER);
		List adapters = new ArrayList();
		adapters.add("-- select an adapter --");
		adapters.addAll(startedAdapters);	
		pipeLineTestForm.set("adapters", adapters);
	
	    // Forward control to the specified success URI
	    log.debug("forward to success");
	    return (mapping.findForward("success"));
	}
}
