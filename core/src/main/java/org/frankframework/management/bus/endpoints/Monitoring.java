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
package org.frankframework.management.bus.endpoints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import jakarta.annotation.security.RolesAllowed;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ValidationException;
import org.frankframework.management.bus.ActionSelector;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.management.bus.dto.MonitorDTO;
import org.frankframework.management.bus.dto.TriggerDTO;
import org.frankframework.management.bus.message.EmptyMessage;
import org.frankframework.management.bus.message.JsonMessage;
import org.frankframework.management.bus.message.StringMessage;
import org.frankframework.monitoring.AdapterFilter;
import org.frankframework.monitoring.EventThrowing;
import org.frankframework.monitoring.EventType;
import org.frankframework.monitoring.ITrigger;
import org.frankframework.monitoring.Monitor;
import org.frankframework.monitoring.MonitorException;
import org.frankframework.monitoring.MonitorManager;
import org.frankframework.monitoring.Severity;
import org.frankframework.monitoring.SourceFiltering;
import org.frankframework.monitoring.Trigger;
import org.frankframework.monitoring.events.ConsoleMonitorEvent;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.JacksonUtils;
import org.frankframework.util.SpringUtils;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.MONITORING)
public class Monitoring extends BusEndpointBase {
	public static final String MONITOR_NAME_KEY = "monitor";
	public static final String TRIGGER_NAME_KEY = "trigger";

	private MonitorManager getMonitorManager(String configurationName) {
		ApplicationContext applicationContext = getConfigurationByName(configurationName);
		return applicationContext.getBean("monitorManager", MonitorManager.class);
	}

	private Monitor getMonitor(MonitorManager mm, String monitorName) {
		return mm.findMonitor(monitorName).orElseThrow(()-> new BusException("monitor not found"));
	}

	private ITrigger getTrigger(Monitor monitor, int triggerId) {
		ITrigger trigger = monitor.getTrigger(triggerId);
		if(trigger == null) {
			throw new BusException("trigger not found");
		}
		return trigger;
	}

	@ActionSelector(BusAction.GET)
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	public Message<String> getMonitors(Message<?> message) {
		boolean showConfigAsXml = BusMessageUtils.getBooleanHeader(message, "xml", false);
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);
		String monitorName = BusMessageUtils.getHeader(message, MONITOR_NAME_KEY, null);
		Integer triggerId = BusMessageUtils.getIntHeader(message, TRIGGER_NAME_KEY, null);

		MonitorManager mm = getMonitorManager(configurationName);

		if(monitorName == null) {
			return getMonitors(mm, showConfigAsXml);
		}

		Monitor monitor = getMonitor(mm, monitorName);
		if(triggerId != null) {
			ITrigger trigger = getTrigger(monitor, triggerId);
			return getTrigger(mm, trigger);
		}

		return getMonitor(mm, monitor, showConfigAsXml);
	}

	@ActionSelector(BusAction.UPLOAD)
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	public Message<String> addMonitorOrTrigger(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);
		String name = BusMessageUtils.getHeader(message, MONITOR_NAME_KEY, null); //when present update Trigger

		MonitorManager mm = getMonitorManager(configurationName);

		Monitor monitor;
		if(name != null) {
			monitor = getMonitor(mm, name);
			ITrigger trigger = SpringUtils.createBean(mm.getApplicationContext(), Trigger.class);
			try {
				updateTrigger(trigger, message);
				monitor.addTrigger(trigger);
			} catch (ValidationException e) {
				// Rethrowing this as Warning / Bad Request
				log.info("Trigger failed validation", e);
				throw new BusException("trigger not added, validation failed: " + e.getMessage());
			}
		} else {
			monitor = SpringUtils.createBean(getApplicationContext(), Monitor.class);
			updateMonitor(monitor, message);
			mm.addMonitor(monitor);
		}

		try {
			monitor.configure();
		} catch (ConfigurationException e) {
			// If we cannot configure the monitor despite validation passing, rethrowing it as internal server error.
			throw new BusException("unable to (re)configure Monitor: " + e.getMessage(), e);
		}

		return EmptyMessage.created();
	}

	@ActionSelector(BusAction.DELETE)
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	public Message<String> deleteMonitor(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);
		String monitorName = BusMessageUtils.getHeader(message, MONITOR_NAME_KEY, null);
		Integer triggerId = BusMessageUtils.getIntHeader(message, TRIGGER_NAME_KEY, null);

		MonitorManager mm = getMonitorManager(configurationName);
		Monitor monitor = getMonitor(mm, monitorName);

		if(triggerId != null) {
			ITrigger trigger = getTrigger(monitor, triggerId);
			log.info("removing trigger [{}]", trigger);
			monitor.removeTrigger(trigger);
		} else {
			log.info("removing monitor [{}]", monitor);
			mm.removeMonitor(monitor);
		}

		return EmptyMessage.accepted();
	}

	@ActionSelector(BusAction.MANAGE)
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	public Message<String> updateMonitorOrTrigger(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);
		String monitorName = BusMessageUtils.getHeader(message, MONITOR_NAME_KEY);
		Integer triggerId = BusMessageUtils.getIntHeader(message, TRIGGER_NAME_KEY, null); //when present update Trigger

		MonitorManager mm = getMonitorManager(configurationName);
		Monitor monitor = getMonitor(mm, monitorName);

		if(triggerId != null) {
			ITrigger trigger = getTrigger(monitor, triggerId);
			updateTrigger(trigger, message);
		} else {
			String state = BusMessageUtils.getHeader(message, "state", "edit"); // raise / clear / edit
			if("edit".equals(state)) {
				updateMonitor(monitor, message);
			} else {
				changeMonitorState(monitor, "raise".equals(state));
			}
		}

		return EmptyMessage.accepted();
	}

	private void updateTrigger(ITrigger trigger, Message<?> message) {
		TriggerDTO dto = JacksonUtils.convertToDTO(message.getPayload(), TriggerDTO.class);

		if (dto.getEvents() != null) {
			trigger.setEventCodes(dto.getEvents());
		}
		if (dto.getType() != null) {
			trigger.setTriggerType(dto.getType());
		}
		if (dto.getSeverity() != null) {
			trigger.setSeverity(dto.getSeverity());
		}
		if (dto.getThreshold() != null) {
			trigger.setThreshold(dto.getThreshold());
		}
		if (dto.getPeriod() != null) {
			trigger.setPeriod(dto.getPeriod());
		}
		if (dto.getFilter() != null) {
			trigger.setSourceFiltering(dto.getFilter());

			trigger.clearAdapterFilters();
			if (SourceFiltering.ADAPTER == dto.getFilter()) {
				for (String adapter : dto.getAdapters()) {
					AdapterFilter adapterFilter = new AdapterFilter();
					adapterFilter.setAdapter(adapter);
					trigger.addAdapterFilter(adapterFilter);
				}
			} else if (SourceFiltering.SOURCE == dto.getFilter()) {
				for (Map.Entry<String, List<String>> entry : dto.getSources().entrySet()) {
					AdapterFilter adapterFilter = new AdapterFilter();
					adapterFilter.setAdapter(entry.getKey());
					for (String subObject : entry.getValue()) {
						adapterFilter.addSubObjectText(subObject);
					}
					trigger.addAdapterFilter(adapterFilter);
				}
			}
		}
	}

	private void changeMonitorState(Monitor monitor, boolean raiseMonitor) {
		try {
			log.info("{} monitor [{}]", ()->(raiseMonitor?"raising":"clearing"), monitor::getName);
			String userPrincipalName = BusMessageUtils.getUserPrincipalName();
			monitor.changeState(raiseMonitor, Severity.WARNING, new ConsoleMonitorEvent(userPrincipalName));
		} catch (MonitorException e) {
			throw new BusException("Failed to change monitor state", e);
		}
	}

	private void updateMonitor(Monitor monitor, Message<?> message) {
		MonitorDTO dto = JacksonUtils.convertToDTO(message.getPayload(), MonitorDTO.class);
		if(StringUtils.isNotBlank(dto.getName())) {
			monitor.setName(dto.getName());
		}
		if(dto.getType() != null) {
			monitor.setType(dto.getType());
		}
		if(dto.getDestinations() != null) {
			monitor.setDestinations(String.join(",", dto.getDestinations()));
		}
	}

	private Message<String> getMonitors(MonitorManager mm, boolean showConfigAsXml) {
		if(showConfigAsXml) {
			String xml = mm.toXml().asXmlString();
			return new StringMessage(xml, MediaType.APPLICATION_XML);
		}

		List<Map<String, Object>> monitors = new ArrayList<>();
		for(int i = 0; i < mm.getMonitors().size(); i++) {
			Monitor monitor = mm.getMonitor(i);

			monitors.add(mapMonitor(monitor));
		}

		Map<String, Object> returnMap = new HashMap<>();
		returnMap.put("monitors", monitors);
		returnMap.put("eventTypes", EnumUtils.getEnumList(EventType.class));
		returnMap.put("destinations", mm.getDestinations().keySet());

		return new JsonMessage(returnMap);
	}

	private Message<String> getTrigger(MonitorManager manager, ITrigger trigger) {
		Map<String, Object> returnMap = new HashMap<>();
		returnMap.put("trigger", mapTrigger(trigger));
		returnMap.put("severities", EnumUtils.getEnumList(Severity.class));
		returnMap.put("events", manager.getEvents());

		return new JsonMessage(returnMap);
	}

	private Message<String> getMonitor(MonitorManager manager, Monitor monitor, boolean showConfigAsXml) {
		if(showConfigAsXml) {
			String xml = monitor.toXml().asXmlString();
			return new StringMessage(xml, MediaType.APPLICATION_XML);
		}

		Map<String, Object> monitorInfo = mapMonitor(monitor);

		monitorInfo.put("severities", EnumUtils.getEnumList(Severity.class));
		monitorInfo.put("events", manager.getEvents());

		return new JsonMessage(monitorInfo);
	}

	private Map<String, Object> mapMonitor(Monitor monitor) {
		Map<String, Object> monitorMap = new HashMap<>();
		monitorMap.put("name", monitor.getName());
		monitorMap.put("type", monitor.getType());
		monitorMap.put("destinations", monitor.getDestinationSet());
		monitorMap.put("lastHit", monitor.getLastHit() != null ? monitor.getLastHit().toEpochMilli() : null);

		boolean isRaised = monitor.isRaised();
		monitorMap.put("raised", isRaised);
		monitorMap.put("changed", monitor.getStateChanged() != null ? monitor.getStateChanged().toEpochMilli() : null);
		monitorMap.put("hits", monitor.getAdditionalHitCount());

		if(isRaised) {
			Map<String, Object> alarm = new HashMap<>();
			alarm.put("severity", monitor.getAlarmSeverity());
			EventThrowing source = monitor.getRaisedBy();
			if(source != null) {
				String name = "";
				if(source.getAdapter() != null) {
					name = "%s / %s".formatted(source.getAdapter().getName(), source.getEventSourceName());
				} else {
					name = source.getEventSourceName();
				}
				alarm.put("source", name);
			}
			monitorMap.put("alarm", alarm);
		}

		List<Map<String, Object>> triggers = new ArrayList<>();
		List<ITrigger> listOfTriggers = monitor.getTriggers();
		for (ListIterator<ITrigger> iterator = listOfTriggers.listIterator(); iterator.hasNext(); ) {
			int triggerIndex = iterator.nextIndex();
			ITrigger trigger = iterator.next();

			Map<String, Object> map = mapTrigger(trigger);
			map.put("id", triggerIndex);

			triggers.add(map);
		}
		monitorMap.put("triggers", triggers);
		monitorMap.put("destinations", new ArrayList<>(monitor.getDestinationSet()));
		return monitorMap;
	}

	private Map<String, Object> mapTrigger(ITrigger trigger) {
		Map<String, Object> triggerMap = new HashMap<>();

		triggerMap.put("type", trigger.getTriggerType().name());
		triggerMap.put("events", trigger.getEventCodes());
		triggerMap.put("severity", trigger.getSeverity());
		triggerMap.put("threshold", trigger.getThreshold());
		triggerMap.put("period", trigger.getPeriod());

		if (trigger.getAdapterFilters() != null) {
			Map<String, List<String>> sources = new HashMap<>();
			if (trigger.getSourceFiltering() != SourceFiltering.NONE) {
				for (Map.Entry<String, AdapterFilter> entry : trigger.getAdapterFilters().entrySet()) {
					String adapterName = entry.getKey();
					AdapterFilter af = entry.getValue();
					sources.put(adapterName, af.getSubObjectList());
				}
			}
			triggerMap.put("filter", trigger.getSourceFiltering());
			triggerMap.put("sources", sources);
		}
		return triggerMap;
	}
}
