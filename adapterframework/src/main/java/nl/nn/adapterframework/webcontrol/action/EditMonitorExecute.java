/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
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
 * @version $Id$
 */
public class EditMonitorExecute extends EditMonitor {

	public String determineExitForward(DynaActionForm monitorForm) {
		return (String)monitorForm.get("return");
	}

	public String performAction(DynaActionForm monitorForm, String action, int index, int triggerIndex, HttpServletResponse response) {
		
		MonitorManager mm = MonitorManager.getInstance();
		if (index>=0) {
			Monitor monitor = mm.getMonitor(index);
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
				return determineExitForward(monitorForm);
			} else   
			if (action.equals("OK") || action.equals("Apply")) {
				Monitor formMonitor = (Monitor)monitorForm.get("monitor");
				monitor.setName(formMonitor.getName());
				monitor.setTypeEnum(formMonitor.getTypeEnum());
				monitor.setDestinations(formMonitor.getDestinations());
				if (action.equals("OK")) {
					return determineExitForward(monitorForm);
				} 
			}
		}
		return "self";
	}
}
