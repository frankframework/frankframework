/*
 * $Log: EditTrigger.java,v $
 * Revision 1.2  2008-07-24 12:42:10  europe\L190409
 * rework of monitoring
 *
 * Revision 1.1  2008/07/17 16:21:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
public class EditTrigger extends EditMonitor {

	public void performAction(DynaActionForm monitorForm, String action, int index, int triggerIndex) {
		MonitorManager mm = MonitorManager.getInstance();
	
		if (index>=0) {
			Monitor monitor = mm.getMonitor(index);
			monitorForm.set("monitor",monitor);
			if (triggerIndex>=0) {
				Trigger trigger = monitor.getTrigger(triggerIndex);
				monitorForm.set("trigger",trigger);
			}
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
		List triggerTypes = new ArrayList(); {
			triggerTypes.add("Alarm");
			triggerTypes.add("Clearing");
		}
		monitorForm.set("triggerTypes",triggerTypes);
		monitorForm.set("eventCodes",mm.getEventCodes(mm.findThrower(null)));

	}
}
