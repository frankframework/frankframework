/*
 * $Log: EditMonitor.java,v $
 * Revision 1.4  2008-08-07 11:32:30  europe\L190409
 * rework
 *
 * Revision 1.3  2008/07/24 12:42:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.monitoring.EventThrowing;
import nl.nn.adapterframework.monitoring.EventTypeEnum;
import nl.nn.adapterframework.monitoring.Monitor;
import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.monitoring.SeverityEnum;
import nl.nn.adapterframework.monitoring.Trigger;

import org.apache.struts.action.DynaActionForm;



/**
 * Edit a Monitor - display the form.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class EditMonitor extends ShowMonitors {

	protected String performAction(DynaActionForm monitorForm, String action, int index, int triggerIndex, HttpServletResponse response) {
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
		return null;
	}

	public String determineExitForward(DynaActionForm monitorForm) {
		return "showmonitors";
	}

}
