/*
 * $Log: EditTriggerExecute.java,v $
 * Revision 1.1  2008-07-24 12:42:10  europe\L190409
 * rework of monitoring
 *
 * Revision 1.1  2008/07/17 16:21:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * work in progess
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.monitoring.Monitor;
import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.monitoring.Trigger;

import org.apache.struts.action.DynaActionForm;



/**
 * Edit a Monitor - process the form.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public final class EditTriggerExecute extends EditTrigger {

	public void performAction(DynaActionForm monitorForm, String action, int index, int triggerIndex) {
		
		MonitorManager mm = MonitorManager.getInstance();
		if (index>=0 && triggerIndex>=0) {
			Monitor monitor = mm.getMonitor(index);
			Trigger trigger = monitor.getTrigger(triggerIndex);
			Trigger formTrigger = (Trigger)monitorForm.get("trigger");
			trigger.setType(formTrigger.getType());
			trigger.setEventCode(formTrigger.getEventCode());
			trigger.setSeverity(formTrigger.getSeverity());
			trigger.setThreshold(formTrigger.getThreshold());
			trigger.setPeriod(formTrigger.getPeriod());
		}
	}
}
