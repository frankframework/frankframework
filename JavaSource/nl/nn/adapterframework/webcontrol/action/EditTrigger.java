/*
 * $Log: EditTrigger.java,v $
 * Revision 1.4  2008-08-13 13:46:57  europe\L190409
 * some bugfixing
 *
 * Revision 1.3  2008/08/07 11:32:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework
 *
 * Revision 1.2  2008/07/24 12:42:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.monitoring.Monitor;
import nl.nn.adapterframework.monitoring.MonitorManager;
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



	public String performAction(DynaActionForm monitorForm, String action, int index, int triggerIndex, HttpServletResponse response) {
		MonitorManager mm = MonitorManager.getInstance();
	
		if (index>=0) {
			Monitor monitor = mm.getMonitor(index);
			monitorForm.set("monitor",monitor);
			if (triggerIndex>=0) {
				Trigger trigger = monitor.getTrigger(triggerIndex);
				monitorForm.set("trigger",trigger);
			}
		}
		List triggerTypes = new ArrayList(); {
			triggerTypes.add("Alarm");
			triggerTypes.add("Clearing");
		}
		monitorForm.set("triggerTypes",triggerTypes);
		
		List eventCodes;
		if (action.equals("Filter Events")) {
			Trigger formTrigger = (Trigger)monitorForm.get("trigger");
			String source = formTrigger.getSource();
			eventCodes=mm.getEventCodes(mm.findThrower(source));
			log.debug("filteredEventCodes.size ["+eventCodes.size()+"]");
		} else {
			eventCodes=mm.getEventCodes(mm.findThrower(null));
		}
		log.debug("eventCodes.size ["+eventCodes.size()+"]");
		monitorForm.set("eventCodes",eventCodes);

		List sources;
		if (action.equals("Filter Sources")) {
			Trigger formTrigger = (Trigger)monitorForm.get("trigger");
			String eventCode = formTrigger.getEventCode();
			sources=mm.getEventSourceNames(eventCode);
		} else {
			sources=mm.getEventSourceNames(null);
		}
		log.debug("sources.size ["+sources.size()+"]");
		monitorForm.set("sources",sources);

		return null;
	}
}
