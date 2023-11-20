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
package nl.nn.adapterframework.monitoring;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.monitoring.events.FireMonitorEvent;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * A Trigger that has its type configured at startup. Either use type = ALARM or type = CLEARING.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 *
 */
public class Trigger implements ITrigger {
	protected Logger log = LogUtil.getLogger(this);

	private static final String CLASS_NAME_ALARM = Alarm.class.getName();
	private static final String CLASS_NAME_CLEARING = Clearing.class.getName();

	private @Getter Monitor monitor;
	private @Getter @Setter Severity severity;
	private @Getter @Setter SourceFiltering sourceFiltering = SourceFiltering.NONE;
	private @Getter @Setter TriggerType triggerType = TriggerType.ALARM;

	private List<String> eventCodes = new ArrayList<>();
	private Map<String, AdapterFilter> adapterFilters = new LinkedHashMap<>();

	private @Getter int threshold = 0;
	private @Getter int period = 0;

	private LinkedList<Instant> eventDates = null;
	private boolean configured = false;

	@Override
	public void configure() throws ConfigurationException {
		if(monitor == null) {
			throw new ConfigurationException("no monitor autowired");
		}

		if (eventCodes.isEmpty()) {
			log.warn("trigger of Monitor [{}] should have at least one eventCode specified", monitor::getName);
		}

		if (threshold > 0) {
			if(period < 1) {
				throw new ConfigurationException("you must define a period when using threshold > 0");
			}
			if (eventDates == null) {
				eventDates = new LinkedList<>();
			}
		} else { // In case of a reconfigure
			eventDates = null;
		}

		configured = true;
	}

	@Override
	public boolean isConfigured() {
		return configured;
	}

	@Override
	public void onApplicationEvent(FireMonitorEvent event) {
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
		return (getAdapterFilters().isEmpty() || (adapter != null && getAdapterFilters().containsKey(adapter.getName())));
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
			Instant firstDate = eventDates.getFirst();
			if ((now.toEpochMilli() - firstDate.toEpochMilli()) > getPeriod() * 1000) {
				eventDates.removeFirst();
				if (log.isDebugEnabled()) log.debug("removed element dated ["+DateUtils.format(firstDate.toEpochMilli())+"]");
			} else {
				break;
			}
		}
	}

	@Override
	public void toXml(XmlBuilder monitor) {
		XmlBuilder trigger=new XmlBuilder("trigger");
		trigger.addAttribute("className", isAlarm() ? CLASS_NAME_ALARM : CLASS_NAME_CLEARING);
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
		for (int i=0; i<eventCodes.size(); i++) {
			XmlBuilder event=new XmlBuilder("event");
			trigger.addSubElement(event);
			event.setValue(eventCodes.get(i));
		}
		if (getAdapterFilters()!=null && getSourceFiltering() != SourceFiltering.NONE) {
			for (Iterator<String> it=getAdapterFilters().keySet().iterator(); it.hasNext(); ) {
				String adapterName = it.next();
				AdapterFilter af = getAdapterFilters().get(adapterName);
				XmlBuilder adapter = new XmlBuilder("adapterfilter");
				trigger.addSubElement(adapter);
				adapter.addAttribute("adapter",adapterName);
				if (isFilterOnLowerLevelObjects()) {
					List<String> subobjectList=af.getSubObjectList();
					if (subobjectList!=null) {
						for(String subObjectName : subobjectList) {
							XmlBuilder sourceXml=new XmlBuilder("source");
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
	public void addEventCode(String code) {
		eventCodes.add(code);
	}

	public void setEventCode(String code) {
		clearEventCodes();
		addEventCode(code);
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

	@Override
	public void registerAdapterFilter(AdapterFilter af) {
		adapterFilters.put(af.getAdapter(),af);
		if(af.isFilteringToLowerLevelObjects()) {
			setSourceFiltering(SourceFiltering.SOURCE);
		} else if (getSourceFiltering() == SourceFiltering.NONE) {
			setSourceFiltering(SourceFiltering.ADAPTER);
		}
	}

	public boolean isFilterOnLowerLevelObjects() {
		return sourceFiltering == SourceFiltering.SOURCE;
	}
	public boolean isFilterOnAdapters() {
		return sourceFiltering == SourceFiltering.ADAPTER;
	}

	@Override
	public void destroy() throws Exception {
		log.info("removing trigger ["+this+"]");
	}

}
