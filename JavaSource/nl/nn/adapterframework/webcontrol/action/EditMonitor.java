/*
 * $Log: EditMonitor.java,v $
 * Revision 1.1  2008-07-14 17:29:47  europe\L190409
 * support for flexibile monitoring
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.monitoring.EventThrowing;
import nl.nn.adapterframework.monitoring.EventTypeEnum;
import nl.nn.adapterframework.monitoring.Monitor;
import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.monitoring.SeverityEnum;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;



/**
 * Test the Pipeline of an adapter.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public final class EditMonitor extends ActionBase {

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
	
	    // Initialize action
	    initAction(request);
	    if (null == config) {
	        return (mapping.findForward("noconfig"));
	    }
	
		DynaActionForm monitorForm = getPersistentForm(mapping, form, request);

		String action 		= getAndSetProperty(request,monitorForm,"action");
		String indexStr  = request.getParameter("index");
		int index=-1;
		if (StringUtils.isNotEmpty(indexStr)) {
			index=Integer.parseInt(indexStr);
		}
	
		MonitorManager mm = MonitorManager.getInstance();
	
		Monitor monitor;
		if ("edit".equals(action)) {
			monitor = mm.getMonitor(index);
		} else {
			monitor = (Monitor)monitorForm.get("monitor");
		}
		
		List sources = new ArrayList();
		sources.add("-- select an event source --");
		for(Iterator it=mm.getThrowerIterator();it.hasNext();) {
			EventThrowing thrower = (EventThrowing)it.next();
			sources.add(thrower.getEventSourceName());
		}
		monitorForm.set("sources",sources);
		monitorForm.set("eventTypes",EventTypeEnum.getEnumList());
		monitorForm.set("severities",SeverityEnum.getEnumList());
		
	    log.debug("forward to success");
	    return (mapping.findForward("success"));
	}
}
