package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.RunStateEnum;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;


/**
 *
 * Test the Pipeline of an adapter
 * @version Id
 * @author  Johan Verrips
 * @see nl.nn.adapterframework.configuration.Configuration
 * @see nl.nn.adapterframework.core.Adapter
 * @see nl.nn.adapterframework.core.PipeLine
 */

public final class TestPipeLine extends ActionBase {
	public static final String version="$Id: TestPipeLine.java,v 1.3 2004-03-26 10:42:58 NNVZNL01#L180564 Exp $";
	


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

    DynaActionForm pipeLineTestForm = getPersistentForm(mapping, form, request);

    ArrayList adapters = new ArrayList();
    adapters.add("-- select an adapter --");

    // get the names of the Adapters
    Iterator adapterNamesIt=config.getRegisteredAdapterNames();
    while (adapterNamesIt.hasNext()){
        String adapterName=(String)adapterNamesIt.next();
        IAdapter adapter=config.getRegisteredAdapter(adapterName);
        // add the adapter if it is started.
        if (adapter.getRunState().equals(RunStateEnum.STARTED)) {
	        adapters.add(adapterName);
        }
        
    }
	pipeLineTestForm.set("adapters", adapters);

    // Forward control to the specified success URI
    log.debug("forward to success");
    return (mapping.findForward("success"));

}
}
