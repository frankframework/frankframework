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
 * @version Id
 * @author  Johan Verrips
 * @see nl.nn.adapterframework.core.Adapter
 */

public final class AdapterHandler extends ActionBase {
		public static final String version="$Id: AdapterHandler.java,v 1.3.6.1 2007-09-13 13:27:20 europe\M00035F Exp $";



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
