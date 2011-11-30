/*
 * $Log: EditTrigger.java,v $
 * Revision 1.7  2011-11-30 13:51:46  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.5  2009/05/13 08:19:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved monitoring: triggers can now be filtered multiselectable on adapterlevel
 *
 * Revision 1.4  2008/08/13 13:46:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
