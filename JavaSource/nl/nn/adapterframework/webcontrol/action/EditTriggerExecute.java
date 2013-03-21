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
public class EditTriggerExecute extends EditMonitorExecute {

	public String performAction(DynaActionForm monitorForm, String action, int index, int triggerIndex, HttpServletResponse response) {
		
		MonitorManager mm = MonitorManager.getInstance();
		
		if (index>=0 && triggerIndex>=0) {
			Monitor monitor = mm.getMonitor(index);
			Trigger trigger = monitor.getTrigger(triggerIndex);
			Trigger formTrigger = (Trigger)monitorForm.get("trigger");
			log.debug("copying form trigger ("+formTrigger.hashCode()+") values to trigger["+triggerIndex+"] ("+trigger.hashCode()+")");
			trigger.setType(formTrigger.getType());
			trigger.setEventCodes(formTrigger.getEventCodes());
			trigger.setSourceFiltering(formTrigger.getSourceFiltering());
			if (formTrigger.isFilterOnAdapters()) {
				log.debug("setting trigger.adapters from selAdapters");
				trigger.setAdapters((String[])monitorForm.get("selAdapters"));
			}
			if (formTrigger.isFilterOnLowerLevelObjects()) {
				log.debug("setting trigger.sources from selSources");
				trigger.setSources((String[])monitorForm.get("selSources"));
			}
			trigger.setFilterExclusive(formTrigger.isFilterExclusive());
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
