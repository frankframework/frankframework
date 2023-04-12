/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.adapterframework.management.bus.endpoints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.management.bus.ActionSelector;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.JsonResponseMessage;
import nl.nn.adapterframework.management.bus.StringResponseMessage;
import nl.nn.adapterframework.management.bus.TopicSelector;
import nl.nn.adapterframework.monitoring.AdapterFilter;
import nl.nn.adapterframework.monitoring.EventThrowing;
import nl.nn.adapterframework.monitoring.EventType;
import nl.nn.adapterframework.monitoring.ITrigger;
import nl.nn.adapterframework.monitoring.Monitor;
import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.monitoring.SourceFiltering;
import nl.nn.adapterframework.util.EnumUtils;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.MONITORING)
public class Monitoring extends BusEndpointBase {

	private MonitorManager getMonitorManager(String configurationName) {
		ApplicationContext applicationContext = getConfigurationByName(configurationName);
		return applicationContext.getBean("monitorManager", MonitorManager.class);
	}

	@ActionSelector(BusAction.GET)
	public Message<String> getMonitors(Message<?> message) {
		boolean showConfigXml = BusMessageUtils.getBooleanHeader(message, "xml", false);
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);

		Map<String, Object> returnMap = new HashMap<>();
		MonitorManager mm = getMonitorManager(configurationName);

		if(showConfigXml) {
			String xml = mm.toXml().toXML();
			return new StringResponseMessage(xml, MediaType.APPLICATION_XML);
		}

		List<Map<String, Object>> monitors = new ArrayList<>();
		for(int i = 0; i < mm.getMonitors().size(); i++) {
			Monitor monitor = mm.getMonitor(i);

			monitors.add(mapMonitor(monitor));
		}

		returnMap.put("monitors", monitors);
		returnMap.put("enabled", mm.isEnabled());
		returnMap.put("eventTypes", EnumUtils.getEnumList(EventType.class));
		returnMap.put("destinations", mm.getDestinations().keySet());

		return new JsonResponseMessage(returnMap);
	}

	private Map<String, Object> mapMonitor(Monitor monitor) {
		Map<String, Object> monitorMap = new HashMap<>();
		monitorMap.put("name", monitor.getName());
		monitorMap.put("type", monitor.getType());
		monitorMap.put("destinations", monitor.getDestinationSet());
		monitorMap.put("lastHit", monitor.getLastHit());

		boolean isRaised = monitor.isRaised();
		monitorMap.put("raised", isRaised);
		monitorMap.put("changed", monitor.getStateChangeDt());
		monitorMap.put("hits", monitor.getAdditionalHitCount());

		if(isRaised) {
			Map<String, Object> alarm = new HashMap<>();
			alarm.put("severity", monitor.getAlarmSeverity());
			EventThrowing source = monitor.getAlarmSource();
			if(source != null) {
				String name = "";
				if(source.getAdapter() != null) {
					name = String.format("%s / %s", source.getAdapter().getName(), source.getEventSourceName());
				} else {
					name = source.getEventSourceName();
				}
				alarm.put("source", name);
			}
			monitorMap.put("alarm", alarm);
		}

		List<Map<String, Object>> triggers = new ArrayList<Map<String, Object>>();
		List<ITrigger> listOfTriggers = monitor.getTriggers();
		for(ITrigger trigger : listOfTriggers) {

			Map<String, Object> map = mapTrigger(trigger);
			map.put("id", listOfTriggers.indexOf(trigger));

			triggers.add(map);
		}
		monitorMap.put("triggers", triggers);

		List<String> destinations = new ArrayList<>();
		Set<String> d = monitor.getDestinationSet();
		for(Iterator<String> it = d.iterator(); it.hasNext();) {
			destinations.add(it.next());
		}
		monitorMap.put("destinations", destinations);

		return monitorMap;
	}

	private Map<String, Object> mapTrigger(ITrigger trigger) {
		Map<String, Object> triggerMap = new HashMap<>();

		triggerMap.put("type", trigger.getTriggerType().name());
		triggerMap.put("events", trigger.getEventCodes());
		triggerMap.put("severity", trigger.getSeverity());
		triggerMap.put("threshold", trigger.getThreshold());
		triggerMap.put("period", trigger.getPeriod());

		if(trigger.getAdapterFilters() != null) {
			Map<String, List<String>> sources = new HashMap<>();
			if(trigger.getSourceFiltering() != SourceFiltering.NONE) {
				for(Iterator<String> it1 = trigger.getAdapterFilters().keySet().iterator(); it1.hasNext();) {
					String adapterName = it1.next();

					AdapterFilter af = trigger.getAdapterFilters().get(adapterName);
					sources.put(adapterName, af.getSubObjectList());
				}
			}
			triggerMap.put("filter", trigger.getSourceFiltering());
			triggerMap.put("sources", sources);
		}
		return triggerMap;
	}
}
