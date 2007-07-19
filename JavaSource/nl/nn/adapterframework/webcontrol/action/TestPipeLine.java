/*
 * $Log: TestPipeLine.java,v $
 * Revision 1.4  2007-07-19 15:15:49  europe\L190409
 * list Adapters in order of configuration
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.util.ArrayList;

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
	public static final String version="$RCSfile: TestPipeLine.java,v $ $Revision: 1.4 $ $Date: 2007-07-19 15:15:49 $";

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
	
	    // Initialize action
	    initAction(request);
	    if (null == config) {
	        return (mapping.findForward("noconfig"));
	    }
	
	    DynaActionForm pipeLineTestForm = getPersistentForm(mapping, form, request);
	
	    ArrayList adapters = new ArrayList();
	    adapters.add("-- select an adapter --");
	
		for(int i=0; i<config.getRegisteredAdapters().size(); i++) {
			IAdapter adapter = config.getRegisteredAdapter(i);
	        // add the adapter if it is started.
	        if (adapter.getRunState().equals(RunStateEnum.STARTED)) {
		        adapters.add(adapter.getName());
	        }
	    }
		pipeLineTestForm.set("adapters", adapters);
	
	    // Forward control to the specified success URI
	    log.debug("forward to success");
	    return (mapping.findForward("success"));
	}
}
