/*
 * $Log: EditMonitorExecute.java,v $
 * Revision 1.1  2008-07-17 16:21:49  europe\L190409
 * work in progess
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.monitoring.Monitor;
import nl.nn.adapterframework.monitoring.MonitorManager;

import org.apache.struts.action.DynaActionForm;



/**
 * Edit a Monitor - process the form.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public final class EditMonitorExecute extends EditMonitor {

	public void performAction(DynaActionForm monitorForm, String action, int index, int triggerIndex) {
		MonitorManager mm = MonitorManager.getInstance();
	
		if (index>=0) {
			Monitor monitor = mm.getMonitor(index);
			Monitor formMonitor = (Monitor)monitorForm.get("monitor");
			monitor.setName(formMonitor.getName());
			monitor.setTypeEnum(formMonitor.getTypeEnum());
			monitor.setGuardedObject(formMonitor.getGuardedObject());
			monitor.setExport(formMonitor.isExport());
		}
	}
}
