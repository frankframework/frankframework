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

public class TestService extends ActionBase {
	public static final String version="$Id: TestService.java,v 1.1 2004-02-04 08:36:16 a1909356#db2admin Exp $";
	
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
