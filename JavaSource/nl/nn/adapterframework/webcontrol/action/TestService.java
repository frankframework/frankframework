package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.receivers.ServiceDispatcher;
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
 * Testing a service prefill
 * @version Id
 * @author Johan Verrips
 */
public class TestService extends ActionBase {
	public static final String version="$Id: TestService.java,v 1.3 2004-03-26 10:42:57 NNVZNL01#L180564 Exp $";
	
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
	ArrayList services=new ArrayList();
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
