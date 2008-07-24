/*
 * $Log: ShowMonitors.java,v $
 * Revision 1.3  2008-07-24 12:42:10  europe\L190409
 * rework of monitoring
 *
 * Revision 1.2  2008/07/17 16:21:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * work in progess
 *
 * Revision 1.1  2008/07/14 17:29:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for flexibile monitoring
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.monitoring.Monitor;
import nl.nn.adapterframework.monitoring.MonitorException;
import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.util.ClassUtils;

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

	public void debugFormData(HttpServletRequest request, ActionForm form) {
		for (Enumeration enum=request.getParameterNames();enum.hasMoreElements();) {
			String name=(String)enum.nextElement();
			String[] values=request.getParameterValues(name);
			if (values.length==1) {
				log.debug("Parameter ["+name+"] value ["+values[0]+"]");
			} else {
				for (int i=0;i<values.length;i++) {
					log.debug("Parameter ["+name+"]["+i+"] value ["+values[i]+"]");
				}
			}
		}
		if (form instanceof DynaActionForm) {
			DynaActionForm daf=(DynaActionForm)form;
			log.debug("class ["+daf.getDynaClass().getName()+"]");
			for (Iterator it=daf.getMap().keySet().iterator();it.hasNext();) {
				String key=(String)it.next();
				Object value=daf.get(key);
				log.debug("key ["+key+"] class ["+ClassUtils.nameOf(value)+"] value ["+value+"]");
				if (value!=null) {
					if (value instanceof Monitor) {
						Monitor monitor=(Monitor)value;
						log.debug("Monitor :"+monitor.toXml(-1,true).toXML());		
					}
				}
			}
		}
	}


	protected void performAction(String action, int index, DynaActionForm form, HttpServletResponse response) throws MonitorException {
		log.debug("should performing action ["+action+"] on monitorName nr ["+index+"]");
	}


	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		// Initialize action
		initAction(request);

		if (null==config) {
			return (mapping.findForward("noconfig"));
		}

		DynaActionForm monitorForm = getPersistentForm(mapping, form, request);

		debugFormData(request, monitorForm);

		String action 	 = request.getParameter("action");
		String indexStr  = request.getParameter("index");
		int index=-1;
		if (StringUtils.isNotEmpty(indexStr)) {
			index=Integer.parseInt(indexStr);
		}
		try {
			performAction(action, index, monitorForm, response);
		} catch (MonitorException e) {
			error("could not perform action ["+action+"] on monitor nr ["+index+"]", e);
		}
		
		MonitorManager mm = MonitorManager.getInstance();

		monitorForm.set("monitors",mm.getMonitors());
		monitorForm.set("allDestinations",mm.getDestinations().keySet());
		List destinations=new ArrayList();
		for (int i=0;i<mm.getMonitors().size();i++) {
			Monitor m=mm.getMonitor(i);
			Set d=m.getDestinationSet();
			for (Iterator it=d.iterator();it.hasNext();) {
				destinations.add(i+","+it.next());				
			}
		}
		String[] selDest=new String[destinations.size()];
		selDest=(String[])destinations.toArray(selDest);
		monitorForm.set("selDestinations",selDest);

		// Forward control to the specified success URI
		log.debug("forward to success");
		return (mapping.findForward("success"));
	}
}
