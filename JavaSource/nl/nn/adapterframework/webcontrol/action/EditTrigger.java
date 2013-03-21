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
 * @version $Id$
 */
public class EditTrigger extends EditMonitor {

	public static final String LABEL_FILTER_EVENTS2ADAPTERS  =   "Events -> Adapters";
	public static final String LABEL_FILTER_EVENTS2SOURCES   =   "Events -> Sources";
	public static final String LABEL_FILTER_ADAPTERS2EVENTS  = "Adapters -> Events";
	public static final String LABEL_FILTER_ADAPTERS2SOURCES = "Adapters -> Sources";
	public static final String LABEL_FILTER_SOURCES2EVENTS   =  "Sources -> Events";
	public static final String LABEL_FILTER_SOURCES2ADAPTERS =  "Sources -> Adapters";

	public String performAction(DynaActionForm monitorForm, String action, int index, int triggerIndex, HttpServletResponse response) {
		MonitorManager mm = MonitorManager.getInstance();
		Monitor monitor=null;
		Trigger trigger=null;
		
		if (index>=0) {
			monitor = mm.getMonitor(index);
			monitorForm.set("monitor",monitor);
			if (triggerIndex>=0) {
				trigger = monitor.getTrigger(triggerIndex);
				monitorForm.set("trigger",trigger);
			}
		}
		List triggerTypes = new ArrayList(); {
			triggerTypes.add("Alarm");
			triggerTypes.add("Clearing");
		}
		monitorForm.set("triggerTypes",triggerTypes);

//		Trigger formTrigger = (Trigger)monitorForm.get("trigger");		
		
		List eventCodes;
		if (action.equals(LABEL_FILTER_ADAPTERS2EVENTS)) {
			log.debug("performAction() "+LABEL_FILTER_ADAPTERS2EVENTS);
			eventCodes=mm.getEventCodesByAdapters((String[])monitorForm.get("selAdapters"));
		} else 	{	
			if (action.equals(LABEL_FILTER_SOURCES2EVENTS)) {
				log.debug("performAction() "+LABEL_FILTER_SOURCES2EVENTS);
				trigger.setSources((String[])monitorForm.get("selSources"));
				eventCodes=mm.getEventCodesBySources(trigger.getSourceList());
			} else {
				eventCodes=mm.getEventCodesBySources(mm.getThrowers());
			}
		}
		monitorForm.set("eventCodes",eventCodes);

		List adapters;
		if (action.equals(LABEL_FILTER_EVENTS2ADAPTERS)) {
			log.debug("performAction() "+LABEL_FILTER_EVENTS2ADAPTERS);
			trigger.setSourceFiltering(Trigger.SOURCE_FILTERING_BY_ADAPTER);
			adapters=mm.getAdapterNamesByEventCodes(trigger.getEventCodeList());
		} else 	{	
			if (action.equals(LABEL_FILTER_SOURCES2ADAPTERS)) {
				log.debug("performAction() "+LABEL_FILTER_SOURCES2ADAPTERS);
				trigger.setSourceFiltering(Trigger.SOURCE_FILTERING_BY_ADAPTER);
				trigger.setSources((String[])monitorForm.get("selSources"));
				adapters=mm.getAdapterNamesBySources(trigger.getSourceList());
			} else {
				adapters=mm.getAdapterNames();
			}				
		}
		monitorForm.set("adapters",adapters);
		if (!action.equals(LABEL_FILTER_ADAPTERS2EVENTS) &&
			!action.equals(LABEL_FILTER_ADAPTERS2SOURCES)) {
			monitorForm.set("selAdapters",trigger.getAdapters());
		}

		List sources;
		if (action.equals(LABEL_FILTER_EVENTS2SOURCES)) {
			log.debug("performAction() "+LABEL_FILTER_EVENTS2SOURCES);
			trigger.setSourceFiltering(Trigger.SOURCE_FILTERING_BY_LOWER_LEVEL_OBJECT);
			sources=mm.getEventSourceNamesByEventCodes(trigger.getEventCodeList());
		} else {
			if (action.equals(LABEL_FILTER_ADAPTERS2SOURCES)) {
				log.debug("performAction() "+LABEL_FILTER_ADAPTERS2SOURCES);
				trigger.setSourceFiltering(Trigger.SOURCE_FILTERING_BY_LOWER_LEVEL_OBJECT);
				sources=mm.getEventSourceNamesByAdapters((String[])monitorForm.get("selAdapters"));
			} else {
				sources=mm.getEventSourceNamesByEventCodes(null);
			}
		}
		monitorForm.set("sources",sources);
		if (!action.equals(LABEL_FILTER_SOURCES2EVENTS) &&
			!action.equals(LABEL_FILTER_SOURCES2ADAPTERS)) {
			monitorForm.set("selSources",trigger.getSources());
		}
		

		return null;
	}
}
