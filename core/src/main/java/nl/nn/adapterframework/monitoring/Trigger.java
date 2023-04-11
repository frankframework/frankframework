/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
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

	public static final int SOURCE_FILTERING_NONE=0;
	public static final int SOURCE_FILTERING_BY_ADAPTER=1;
	public static final int SOURCE_FILTERING_BY_LOWER_LEVEL_OBJECT=2;

	private Monitor monitor;
	private @Getter @Setter Severity severity;
	private SourceFiltering sourceFiltering = SourceFiltering.NONE;
	private @Getter @Setter TriggerType triggerType = TriggerType.ALARM;

	private List<String> eventCodes = new ArrayList<>();
	private Map<String, AdapterFilter> adapterFilters = new LinkedHashMap<>();

	private @Getter int threshold = 0;
	private @Getter int period = 0;

	private LinkedList<Date> eventDates = null;
	private boolean configured = false;

	@Override
	public void configure() {
		if (eventCodes.isEmpty()) {
			log.warn("trigger of Monitor ["+getMonitor().getName()+"] should have at least one eventCode specified");
		}

		if (threshold>0) {
			if (eventDates==null) {
				eventDates = new LinkedList<>();
			}
		} else {
			eventDates=null;
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

	public void evaluateEvent(FireMonitorEvent event) {
		EventThrowing source = event.getSource();
		if(evaluateAdapterFilters(source)) {
			try {
				evaluateEvent(source, event.getEventCode());
			} catch (MonitorException e) {
				throw new IllegalStateException("unable to evaluate trigger for event ["+event.getEventCode()+"]", e);
			}
		}
	}

	protected boolean evaluateAdapterFilters(EventThrowing source) {
		Adapter adapter = source.getAdapter();
		return (getAdapterFilters().isEmpty() || (adapter != null && getAdapterFilters().containsKey(adapter.getName())));
	}

	public void evaluateEvent(EventThrowing source, String eventCode) throws MonitorException {
		boolean alarm = isAlarm();
		if (log.isDebugEnabled()) log.debug("evaluating MonitorEvent ["+source.getEventSourceName()+"]");

		Date now = new Date();
		if (getThreshold()>0) {
			cleanUpEvents(now);
			eventDates.add(now);
			if (eventDates.size() >= getThreshold()) {
				getMonitor().changeState(now, alarm, getSeverity(), source, eventCode, null);
			}
		} else {
			getMonitor().changeState(now, alarm, getSeverity(), source, eventCode, null);
		}
	}

	@Override
	public void clearEvents() {
		if (eventDates!=null) {
			eventDates.clear();
		}
	}

	protected void cleanUpEvents(Date now) {
		while(!eventDates.isEmpty()) {
			Date firstDate = eventDates.getFirst();
			if ((now.getTime() - firstDate.getTime()) > getPeriod() * 1000) {
				eventDates.removeFirst();
				if (log.isDebugEnabled()) log.debug("removed element dated ["+DateUtils.format(firstDate)+"]");
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
		if (getAdapterFilters()!=null) {
			if (getSourceFilteringEnum() != SourceFiltering.NONE) {
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
	}

	@Override
	public void setMonitor(Monitor monitor) {
		this.monitor = monitor;
	}
	public Monitor getMonitor() {
		return monitor;
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
	public void setEventCodes(String[] arr) {
		clearEventCodes();
		for (int i=0;i<arr.length;i++) {
			addEventCode(arr[i]);
		}
	}

	@Override
	public String[] getEventCodes() {
		return eventCodes.toArray(new String[eventCodes.size()]);
	}

	public List<String> getEventCodeList() {
		return eventCodes;
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
		setSourceFilteringEnum(SourceFiltering.NONE);
	}

	@Override
	public void registerAdapterFilter(AdapterFilter af) {
		adapterFilters.put(af.getAdapter(),af);
		if(af.isFilteringToLowerLevelObjects()) {
			setSourceFilteringEnum(SourceFiltering.SOURCE);
		} else if (getSourceFilteringEnum() == SourceFiltering.NONE) {
			setSourceFilteringEnum(SourceFiltering.ADAPTER);
		}
	}

	public boolean isFilterOnLowerLevelObjects() {
		return sourceFiltering == SourceFiltering.SOURCE;
	}
	public boolean isFilterOnAdapters() {
		return sourceFiltering == SourceFiltering.ADAPTER;
	}

	@Override
	public void setSourceFilteringEnum(SourceFiltering filtering) {
		this.sourceFiltering = filtering;
	}

	@Override
	public String getSourceFiltering() {
		return sourceFiltering.name().toLowerCase();
	}

	@Override
	public SourceFiltering getSourceFilteringEnum() {
		return sourceFiltering;
	}

	@Override
	public void destroy() throws Exception {
		log.info("removing trigger ["+this+"]");
	}

}
