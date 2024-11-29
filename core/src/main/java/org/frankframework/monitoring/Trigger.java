/*
   Copyright 2013 Nationale-Nederlanden, 2021-2023 WeAreFrank!

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
package org.frankframework.monitoring;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import jakarta.annotation.Nonnull;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ValidationException;
import org.frankframework.core.Adapter;
import org.frankframework.monitoring.events.FireMonitorEvent;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.XmlBuilder;

/**
 * A Trigger that has its type configured at startup. Either use type = ALARM or type = CLEARING.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 *
 */
@Log4j2
public class Trigger implements ITrigger {
	// The element names, which can be used as a Trigger.
	private static final String ALARM_NAME = "AlarmTrigger";
	private static final String CLEARING_NAME = "ClearingTrigger";

	private @Getter Monitor monitor;
	private @Getter @Setter Severity severity;
	private @Getter @Setter SourceFiltering sourceFiltering = SourceFiltering.NONE;
	private @Getter @Setter TriggerType triggerType = TriggerType.ALARM;

	private final List<String> eventCodes = new ArrayList<>();
	private final Map<String, AdapterFilter> adapterFilters = new LinkedHashMap<>();

	private @Getter int threshold = 0;
	private @Getter int period = 0;

	private Queue<Instant> eventDates = null;
	private boolean configured = false;

	/**
	 * Default Trigger constructor
	 */
	public Trigger() {
		// No op
	}

	/**
	 * Copy-constructor for trigger used to validate updates before applying them.
	 *
	 * @param trigger The {@link ITrigger} to be copied.
	 */
	public Trigger(ITrigger trigger) {
		this.severity = trigger.getSeverity();
		this.sourceFiltering = trigger.getSourceFiltering();
		this.triggerType = trigger.getTriggerType();
		this.eventCodes.addAll(trigger.getEventCodes());
		this.threshold = trigger.getThreshold();
		this.period = trigger.getPeriod();
		if (trigger.getAdapterFilters() != null) {
			this.adapterFilters.putAll(trigger.getAdapterFilters());
		}
	}

	@Override
	public void validate() throws ValidationException {
		if (threshold > 0 && period < 1) {
			throw new ValidationException("you must define a period when using threshold > 0");
		}

		if (severity == null) {
			throw new ValidationException("you must define a severity for the trigger");
		}
	}

	@Override
	public void configure() throws ConfigurationException {
		configured = false;
		if (monitor == null) {
			throw new ConfigurationException("no monitor autowired");
		}
		try {
			validate();
		} catch (ValidationException e) {
			throw new ConfigurationException(e);
		}
		if (eventCodes.isEmpty()) {
			log.warn("trigger of Monitor [{}] should have at least one eventCode specified", monitor::getName);
		}

		if (threshold > 0) {
			if (eventDates == null) {
				eventDates = new ArrayDeque<>();
			}
		} else { // In case of a reconfigure
			eventDates = null;
		}

		if (severity == null) {
			throw new ConfigurationException("you must define a severity for the trigger");
		}

		configured = true;
	}

	@Override
	public boolean isConfigured() {
		return configured;
	}

	@Override
	public void onApplicationEvent(@Nonnull FireMonitorEvent event) {
		if(configured && eventCodes.contains(event.getEventCode())) {
			evaluateEvent(event);
		}
	}

	protected void evaluateEvent(FireMonitorEvent event) {
		if(evaluateAdapterFilters(event.getSource())) {
			try {
				changeState(event);
			} catch (MonitorException e) {
				throw new IllegalStateException("unable to evaluate trigger for event ["+event.getEventCode()+"]", e);
			}
		}
	}

	protected boolean evaluateAdapterFilters(EventThrowing source) {
		Adapter adapter = source.getAdapter();
		return getAdapterFilters().isEmpty() || (adapter != null && getAdapterFilters().containsKey(adapter.getName()));
	}

	protected void changeState(FireMonitorEvent event) throws MonitorException {
		boolean alarm = isAlarm();
		log.debug("evaluating MonitorEvent [{}]", event::getEventSourceName);

		if (getThreshold()>0) {
			cleanUpEvents(event.getEventTime());
			eventDates.add(event.getEventTime());
			if (eventDates.size() >= getThreshold()) {
				getMonitor().changeState(alarm, getSeverity(), event);
			}
		} else {
			getMonitor().changeState(alarm, getSeverity(), event);
		}
	}

	@Override
	public void clearEvents() {
		if (eventDates!=null) {
			eventDates.clear();
		}
	}

	protected void cleanUpEvents(Instant now) {
		while(!eventDates.isEmpty()) {
			Instant firstDate = eventDates.peek();
			if ((now.toEpochMilli() - firstDate.toEpochMilli()) > getPeriod() * 1000L) {
				eventDates.poll();
				if (log.isDebugEnabled()) log.debug("removed element dated [{}]", DateFormatUtils.format(firstDate));
			} else {
				break;
			}
		}
	}

	@Override
	public void toXml(XmlBuilder monitor) {
		XmlBuilder trigger=new XmlBuilder(isAlarm() ? ALARM_NAME : CLEARING_NAME);
		monitor.addSubElement(trigger);
		if (getSeverity()!=null) {
			trigger.addAttribute("severity", getSeverity().name());
		}
		if (getThreshold()>0) {
			trigger.addAttribute("threshold",getThreshold());
		}
		if (getPeriod()>0) {
			trigger.addAttribute("period",getPeriod());
		}
		for (String eventCode : eventCodes) {
			XmlBuilder event = new XmlBuilder("Event");
			trigger.addSubElement(event);
			event.setValue(eventCode);
		}
		if (getAdapterFilters()!=null && getSourceFiltering() != SourceFiltering.NONE) {
			for (String adapterName : getAdapterFilters().keySet()) {
				AdapterFilter af = getAdapterFilters().get(adapterName);
				XmlBuilder adapter = new XmlBuilder("Adapterfilter");
				trigger.addSubElement(adapter);
				adapter.addAttribute("adapter", adapterName);
				if (isFilterOnLowerLevelObjects()) {
					List<String> subobjectList = af.getSubObjectList();
					if (subobjectList != null) {
						for (String subObjectName : subobjectList) {
							XmlBuilder sourceXml = new XmlBuilder("source");
							adapter.addSubElement(sourceXml);
							sourceXml.setValue(subObjectName);
						}
					}
				}
			}
		}
	}

	@Override
	public void setMonitor(Monitor monitor) {
		this.monitor = monitor;
	}

	@Override
	public boolean isAlarm() {
		return triggerType == TriggerType.ALARM;
	}

	private void clearEventCodes() {
		eventCodes.clear();
	}
	public void addEventCodeText(String code) {
		eventCodes.add(code);
	}

	public void setEventCode(String code) {
		clearEventCodes();
		addEventCodeText(code);
	}

	@Override
	public void setEventCodes(List<String> events) {
		clearEventCodes();
		eventCodes.addAll(events);
	}

	@Override
	public List<String> getEventCodes() {
		return Collections.unmodifiableList(eventCodes);
	}

	@Override
	public void setThreshold(int i) {
		threshold = i;
	}

	@Override
	public void setPeriod(int i) {
		period = i;
	}

	@Override
	public Map<String, AdapterFilter> getAdapterFilters() {
		return adapterFilters;
	}

	@Override
	public void clearAdapterFilters() {
		adapterFilters.clear();
		setSourceFiltering(SourceFiltering.NONE);
	}

	public boolean isFilterOnLowerLevelObjects() {
		return sourceFiltering == SourceFiltering.SOURCE;
	}

	@Override
	public void addAdapterFilter(AdapterFilter af) {
		adapterFilters.put(af.getAdapter(),af);
		if(af.isFilteringToLowerLevelObjects()) {
			setSourceFiltering(SourceFiltering.SOURCE);
		} else if (getSourceFiltering() == SourceFiltering.NONE) {
			setSourceFiltering(SourceFiltering.ADAPTER);
		}
	}

	public boolean isFilterOnAdapters() {
		return sourceFiltering == SourceFiltering.ADAPTER;
	}

	@Override
	public void destroy() throws Exception {
		log.info("removing trigger [{}]", this);
	}
}
