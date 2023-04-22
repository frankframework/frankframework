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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.management.bus.ActionSelector;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusException;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.EmptyResponseMessage;
import nl.nn.adapterframework.management.bus.JsonResponseMessage;
import nl.nn.adapterframework.management.bus.StringResponseMessage;
import nl.nn.adapterframework.management.bus.TopicSelector;
import nl.nn.adapterframework.monitoring.AdapterFilter;
import nl.nn.adapterframework.monitoring.EventThrowing;
import nl.nn.adapterframework.monitoring.EventType;
import nl.nn.adapterframework.monitoring.ITrigger;
import nl.nn.adapterframework.monitoring.Monitor;
import nl.nn.adapterframework.monitoring.MonitorException;
import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.monitoring.Severity;
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
		boolean showConfigAsXml = BusMessageUtils.getBooleanHeader(message, "xml", false);
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);
		String monitorName = BusMessageUtils.getHeader(message, "monitor", null);
		Integer triggerId = BusMessageUtils.getIntHeader(message, "trigger", null);

		MonitorManager mm = getMonitorManager(configurationName);

		if(monitorName == null) {
			return getMonitors(mm, showConfigAsXml);
		}

		Monitor monitor = mm.findMonitor(monitorName);
		if(monitor == null) {
			throw new BusException("monitor not found");
		}

		if(triggerId != null) {
			ITrigger trigger = monitor.getTrigger(triggerId);
			if(trigger == null) {
				throw new BusException("trigger not found");
			}
			return getTrigger(mm, trigger);
		}

		return getMonitor(monitor, showConfigAsXml);
	}

	@ActionSelector(BusAction.DELETE)
	public Message<String> deleteTrigger(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);
		String monitorName = BusMessageUtils.getHeader(message, "monitor", null);
		Integer triggerId = BusMessageUtils.getIntHeader(message, "trigger", null);

		MonitorManager mm = getMonitorManager(configurationName);
		Monitor monitor = mm.findMonitor(monitorName);

		if(monitor == null) {
			throw new BusException("monitor not found");
		}

		if(triggerId != null) {
			ITrigger trigger = monitor.getTrigger(triggerId);
			if(trigger == null) {
				throw new BusException("trigger not found");
			}
			log.info("removing trigger [{}]", trigger);
			monitor.removeTrigger(trigger);
		} else {
			log.info("removing monitor [{}]", monitor);
			mm.removeMonitor(monitor);
		}

		return EmptyResponseMessage.accepted();
	}

	@ActionSelector(BusAction.MANAGE)
	public Message<String> updateMonitor(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);
		String monitorName = BusMessageUtils.getHeader(message, "monitor");

		MonitorManager mm = getMonitorManager(configurationName);
		Monitor monitor = mm.findMonitor(monitorName);
		if(monitor == null) {
			throw new BusException("monitor not found");
		}

		String action = BusMessageUtils.getHeader(message, "state", "edit");
		switch (action) {
		case "clear":
			try {
				log.info("clearing monitor [" + monitor.getName() + "]");
				monitor.changeState(new Date(), false, Severity.WARNING, null, null, null);
			} catch (MonitorException e) {
				throw new BusException("Failed to change monitor state", e);
			}
			break;
		case "raise":
			try {
				log.info("raising monitor [" + monitor.getName() + "]");
				monitor.changeState(new Date(), true, Severity.WARNING, null, null, null);
			} catch (MonitorException e) {
				throw new BusException("Failed to change monitor state", e);
			}
			break;
		case "edit":
			String name = BusMessageUtils.getHeader(message, "name", null);
			if(StringUtils.isNotBlank(name)) {
				monitor.setName(name);
			}
			EventType type = BusMessageUtils.getEnumHeader(message, "type", EventType.class, null);
			if(type != null) {
				monitor.setType(type);
			}
			String destinations = BusMessageUtils.getHeader(message, "destinations", null);
			if(StringUtils.isNotBlank(destinations)) {
				monitor.setDestinationSet(parseDestinations(destinations));
			}
			break;

		default:
			break;
		}

		return EmptyResponseMessage.accepted();
	}

	private Set<String> parseDestinations(String entry) {
		String[] arr = entry.split(",");
		return new HashSet<>(Arrays.asList(arr));
	}

	private Message<String> getMonitors(MonitorManager mm, boolean showConfigAsXml) {
		if(showConfigAsXml) {
			String xml = mm.toXml().toXML();
			return new StringResponseMessage(xml, MediaType.APPLICATION_XML);
		}

		List<Map<String, Object>> monitors = new ArrayList<>();
		for(int i = 0; i < mm.getMonitors().size(); i++) {
			Monitor monitor = mm.getMonitor(i);

			monitors.add(mapMonitor(monitor));
		}

		Map<String, Object> returnMap = new HashMap<>();
		returnMap.put("monitors", monitors);
		returnMap.put("enabled", mm.isEnabled());
		returnMap.put("eventTypes", EnumUtils.getEnumList(EventType.class));
		returnMap.put("destinations", mm.getDestinations().keySet());

		return new JsonResponseMessage(returnMap);
	}

	private Message<String> getTrigger(MonitorManager manager, ITrigger trigger) {
		Map<String, Object> returnMap = new HashMap<>();
		returnMap.put("trigger", mapTrigger(trigger));
		returnMap.put("severities", EnumUtils.getEnumList(Severity.class));
		returnMap.put("events", manager.getEvents());

		return new JsonResponseMessage(returnMap);
	}

	private Message<String> getMonitor(Monitor monitor, boolean showConfigAsXml) {
		if(showConfigAsXml) {
			String xml = monitor.toXml().toXML();
			return new StringResponseMessage(xml, MediaType.APPLICATION_XML);
		}

		Map<String, Object> monitorInfo = mapMonitor(monitor);
		return new JsonResponseMessage(monitorInfo);
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
