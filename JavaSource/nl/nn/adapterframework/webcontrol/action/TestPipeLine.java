/*
 * $Log: TestPipeLine.java,v $
 * Revision 1.8  2011-11-30 13:51:46  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.6  2008/10/24 14:42:31  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adapters are shown case insensitive sorted
 *
 * Revision 1.5  2007/10/08 13:41:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.4  2007/07/19 15:15:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * list Adapters in order of configuration
 *
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
 * @version Id
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
