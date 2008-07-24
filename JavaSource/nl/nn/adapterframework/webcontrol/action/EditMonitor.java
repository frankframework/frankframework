/*
 * $Log: EditMonitor.java,v $
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.monitoring.EventThrowing;
import nl.nn.adapterframework.monitoring.EventTypeEnum;
import nl.nn.adapterframework.monitoring.Monitor;
import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.monitoring.SeverityEnum;
import nl.nn.adapterframework.monitoring.Trigger;
import nl.nn.adapterframework.util.ClassUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;



/**
 * Edit a Monitor - display the form.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class EditMonitor extends ActionBase {

	public void performAction(DynaActionForm monitorForm, String action, int index, int triggerIndex) {
		MonitorManager mm = MonitorManager.getInstance();
	
		if (index>=0) {
			Monitor monitor = mm.getMonitor(index);
			monitorForm.set("monitor",monitor);
			if (action.equals("createTrigger")) {
				int triggerCount=monitor.getTriggers().size();
				Trigger trigger = new Trigger();
				switch (triggerCount) {
					case 0: trigger.setAlarm(true);
					case 1: trigger.setAlarm(false);
					default: trigger.setAlarm(true);
				}
				monitor.registerTrigger(trigger);				
			} else 
			if (action.equals("deleteTrigger")) {
				monitor.getTriggers().remove(triggerIndex);
			}  
		}
		
		List sources = new ArrayList();
		sources.add("");
		for(Iterator it=mm.getThrowerIterator();it.hasNext();) {
			EventThrowing thrower = (EventThrowing)it.next();
			sources.add(thrower.getEventSourceName());
		}
		monitorForm.set("sources",sources);
		monitorForm.set("eventTypes",EventTypeEnum.getEnumList());
		monitorForm.set("severities",SeverityEnum.getEnumList());
	}

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
	
		// Initialize action
		initAction(request);
		if (null == config) {
			return (mapping.findForward("noconfig"));
		}
		if (isCancelled(request)) {
			log.debug("edit is canceled");
			return (mapping.findForward("success"));
		}
	
		DynaActionForm monitorForm = getPersistentForm(mapping, form, request);


		if (log.isDebugEnabled()) {
			Map map=monitorForm.getMap();
			for (Iterator it=map.keySet().iterator(); it.hasNext();) {
				String key=(String)it.next();
				Object value=map.get(key);
				log.debug("key ["+key+"] class ["+ClassUtils.nameOf(value)+"] value ["+value+"]");
			}
		}

		String action 	      = getAndSetProperty(request,monitorForm,"action");
		String indexStr       = request.getParameter("index");
		String triggerIndexStr = request.getParameter("triggerIndex");
		int index=-1;
		if (StringUtils.isNotEmpty(indexStr)) {
			index=Integer.parseInt(indexStr);
		}
		int triggerIndex=-1;
		if (StringUtils.isNotEmpty(triggerIndexStr)) {
			triggerIndex=Integer.parseInt(triggerIndexStr);
		}
	
		performAction(monitorForm, action,index,triggerIndex);
		
	    log.debug("forward to success");
	    return (mapping.findForward("success"));
	}
}
