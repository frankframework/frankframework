/*
 * $Log: EditTriggerExecute.java,v $
 * Revision 1.3  2008-08-14 14:53:51  europe\L190409
 * fixed exit determination
 *
 * Revision 1.2  2008/08/07 11:32:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework
 *
 * Revision 1.1  2008/07/24 12:42:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework of monitoring
 *
 * Revision 1.1  2008/07/17 16:21:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * work in progess
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import javax.servlet.http.HttpServletResponse;

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
public class EditTriggerExecute extends EditMonitorExecute {

	public String performAction(DynaActionForm monitorForm, String action, int index, int triggerIndex, HttpServletResponse response) {
		
		MonitorManager mm = MonitorManager.getInstance();
		
		if (index>=0 && triggerIndex>=0) {
			Monitor monitor = mm.getMonitor(index);
			Trigger trigger = monitor.getTrigger(triggerIndex);
			Trigger formTrigger = (Trigger)monitorForm.get("trigger");
			trigger.setType(formTrigger.getType());
			trigger.setEventCode(formTrigger.getEventCode());
			trigger.setSource(formTrigger.getSource());
			trigger.setSeverity(formTrigger.getSeverity());
			trigger.setThreshold(formTrigger.getThreshold());
			trigger.setPeriod(formTrigger.getPeriod());
		}
		String result;		
		if (action.equals("OK")) {
			result= determineExitForward(monitorForm);
		} else {
			result= "self";
		}
		log.debug("determined forward ["+result+"] from action ["+action+"] monitorForm.return ["+monitorForm.get("return")+"]");
		return result;
	}

}
