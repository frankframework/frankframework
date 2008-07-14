/*
 * $Log: ShowMonitors.java,v $
 * Revision 1.1  2008-07-14 17:29:47  europe\L190409
 * support for flexibile monitoring
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.monitoring.MonitorException;
import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

/**
 * Show all monitors.
 * 
 * @author	Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class ShowMonitors extends ActionBase {

	protected void performAction(String action, int index, HttpServletResponse response) throws MonitorException {
		log.debug("should performing action ["+action+"] on monitorName nr ["+index+"]");
	}


	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		// Initialize action
		initAction(request);

		if (null==config) {
			return (mapping.findForward("noconfig"));
		}

		DynaActionForm monitorForm = getPersistentForm(mapping, form, request);

		String action 	 = request.getParameter("action");
		String indexStr  = request.getParameter("index");
		int index=-1;
		if (StringUtils.isNotEmpty(indexStr)) {
			index=Integer.parseInt(indexStr);
		}
		if (StringUtils.isNotEmpty(action)) {
			try {
				performAction(action, index, response);
			} catch (MonitorException e) {
				error("could not perform action ["+action+"] on monitor nr ["+index+"]", e);
			}
		}

		XmlBuilder monitors = MonitorManager.getInstance().toXml();
		request.setAttribute("monitoring", monitors.toXML());

		// Forward control to the specified success URI
		log.debug("forward to success");
		return (mapping.findForward("success"));
	}
}
