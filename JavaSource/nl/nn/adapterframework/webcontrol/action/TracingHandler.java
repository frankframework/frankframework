/*
 * $Log: TracingHandler.java,v $
 * Revision 1.1  2006-09-14 15:28:50  europe\L190409
 * first version of TracingHandlers
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.util.TracingUtil;

import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Handles various function for tracing: start, stop.
 * @author  Peter Leeuwenburgh
 */

public final class TracingHandler extends ActionBase {
	public static final String version="$RCSfile: TracingHandler.java,v $ $Revision: 1.1 $ $Date: 2006-09-14 15:28:50 $";

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
		String action = request.getParameter("action");
		if (null == action)
			action = mapping.getParameter();

		log.debug("action [" + action + "]");

		if (action.equals("stoptracing")) {
			try {
				TracingUtil.stopTracing();
			} catch (Throwable t) {
				errors.add("", new ActionError("errors.generic", t));
			}
		} else {
			if (action.equals("starttracing")) {
				try {
					TracingUtil.startTracing();
				} catch (Throwable t) {
					errors.add("", new ActionError("errors.generic", t));
				}
			}
		}

		// Report any errors we have discovered back to the original form
		if (!errors.isEmpty()) {
			saveErrors(request, errors);
		} // Forward control to the specified success URI
		log.debug("forward to success");
		return (mapping.findForward("success"));
	}
}