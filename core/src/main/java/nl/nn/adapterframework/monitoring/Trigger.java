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
package nl.nn.adapterframework.monitoring;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.logging.log4j.Logger;

/**
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class Trigger {
	protected Logger log = LogUtil.getLogger(this);
	
	public static final int SOURCE_FILTERING_NONE=0;
	public static final int SOURCE_FILTERING_BY_ADAPTER=1;
	public static final int SOURCE_FILTERING_BY_LOWER_LEVEL_OBJECT=2;
	
	private Monitor owner;
	private SeverityEnum severity;
	private boolean alarm;

	private List<String> eventCodes = new ArrayList<String>();
	private Map<String, AdapterFilter> adapterFilters = new LinkedHashMap<String, AdapterFilter>();
	
	private int sourceFiltering;
	private boolean filterExclusive = false;
		
	private int threshold=0;
	private int period=0;
	
	private LinkedList<Date> eventDts=null;
		

	public void configure() throws ConfigurationException {
		if (eventCodes.size()==0) {
			log.warn(getLogPrefix()+"configure() trigger of Monitor ["+owner.getName()+"] should have at least one eventCode specified");
		}
		try {
			Map adapterFilterMap = (getSourceFiltering()!=SOURCE_FILTERING_NONE)?adapterFilters:null;
			getOwner().registerEventNotificationListener(this,eventCodes,adapterFilterMap,isFilterOnLowerLevelObjects(), isFilterExclusive());
		} catch (MonitorException e) {
			throw new ConfigurationException(e);
		}
		if (threshold>0) {
			if (eventDts==null) {
				eventDts = new LinkedList<Date>();
			}
		} else {
			eventDts=null;
		}
	}
	
	public String getLogPrefix() {
		return "("+this.hashCode()+") ";
	}

	public void evaluateEvent(EventThrowing source, String eventCode) throws MonitorException {
		Date now = new Date();
		if (getThreshold()>0) {
			cleanUpEvents(now);
			eventDts.add(now);
			if (eventDts.size()>=getThreshold()) {
				getOwner().changeState(now, alarm, getSeverityEnum(), source, eventCode, null);
			}
		} else {
			getOwner().changeState(now, alarm, getSeverityEnum(), source, eventCode, null);
		}
	}	
	
	public void notificationOfReverseTrigger(EventThrowing source) {
		if (eventDts!=null) {
			eventDts.clear();
		}
	}

	protected void cleanUpEvents(Date now) {
		while(eventDts.size()>0) {
			Date firstDate = (Date)eventDts.getFirst();
			if ((now.getTime()-firstDate.getTime())>getPeriod()*1000) {
				eventDts.removeFirst();
				if (log.isDebugEnabled()) log.debug("removed element dated ["+DateUtils.format(firstDate)+"]");
			} else {
				break;
			}
		}
	}

	public void toXml(XmlBuilder monitor) {
		XmlBuilder trigger=new XmlBuilder(isAlarm()?"alarm":"clearing");
		monitor.addSubElement(trigger);
		if (getSeverity()!=null) {
			trigger.addAttribute("severity",getSeverity());
		}
		if (getThreshold()>0) {
			trigger.addAttribute("threshold",getThreshold());
		}
		if (getPeriod()>0) {
			trigger.addAttribute("period",getPeriod());
		}
		XmlBuilder events=new XmlBuilder("events");
		trigger.addSubElement(events);
		for (int i=0; i<eventCodes.size(); i++) {
			XmlBuilder event=new XmlBuilder("event");
			events.addSubElement(event);
			event.setValue((String)eventCodes.get(i));
		}
		if (getAdapterFilters()!=null) {
			XmlBuilder filtersXml=new XmlBuilder("filters");
			filtersXml.addAttribute("filterExclusive",isFilterExclusive());
			trigger.addSubElement(filtersXml);
			if (getSourceFiltering()!=SOURCE_FILTERING_NONE) {
				for (Iterator<String> it=getAdapterFilters().keySet().iterator(); it.hasNext(); ) {
					String adapterName=(String)it.next();
					AdapterFilter af = (AdapterFilter)getAdapterFilters().get(adapterName);
					XmlBuilder adapter=new XmlBuilder("adapterfilter");
					filtersXml.addSubElement(adapter);
					adapter.addAttribute("adapter",adapterName);
					if (isFilterOnLowerLevelObjects()) {
						XmlBuilder sourcesXml=new XmlBuilder("sources");
						adapter.addSubElement(sourcesXml);
						List subobjectList=af.getSubObjectList();
						if (subobjectList!=null) {
							for (Iterator it2 =subobjectList.iterator(); it2.hasNext();) {
								String subObjectName=(String)it2.next();
								XmlBuilder sourceXml=new XmlBuilder("source");
								sourcesXml.addSubElement(sourceXml);
								sourceXml.setValue(subObjectName);
							}
						}
					}
				}
			}
		}
	}

	public void setOwner(Monitor monitor) {
		owner = monitor;
	}
	public Monitor getOwner() {
		return owner;
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}


	public void setAlarm(boolean b) {
		alarm = b;
	}
	public boolean isAlarm() {
		return alarm;
	}

	public String getType() {
		if (isAlarm()) {
			return "Alarm";
		} else {
			return "Clearing";
		}
	}
	public void setType(String type) {
		if (type.equalsIgnoreCase("Alarm")) {
			setAlarm(true);
		}
		if (type.equalsIgnoreCase("Clearing")) {
			setAlarm(false);
		}
	}


	public void clearEventCodes() {
		eventCodes.clear();
	}
	public void addEventCode(String code) {
		eventCodes.add(code);
	}
	
	public void setEventCode(String code) {
		eventCodes.clear();
		addEventCode(code);
	}

	public void setEventCodes(String[] arr) {
		clearEventCodes();
		for (int i=0;i<arr.length;i++) {
			addEventCode(arr[i]);
		}
	}
	public String[] getEventCodes() {
		return (String[])eventCodes.toArray(new String[eventCodes.size()]);
	}

	public List<String> getEventCodeList() {
		return eventCodes;
	}



	/**
	 * set List of all adapters that are present in the FilterMap, to be called from client
	 */
	public void setAdapters(String[] arr) {
		log.debug(getLogPrefix()+"setAdapters()");
		for (Iterator<String> it=adapterFilters.keySet().iterator(); it.hasNext();) {
			String adapterName=(String)it.next();
			boolean found=true;
			for(int i=0; i<arr.length; i++) {
				if (adapterName.equals(arr[i])) {
					break;
				}
				found=false;
			}
			if (!found) {
				log.debug(getLogPrefix()+"setAdapters() removing adapter ["+adapterName+"] from filter");
				it.remove();
			}
		}
		for(int i=0; i<arr.length; i++) {
			String adapterName=arr[i];
			if (!adapterFilters.containsKey(adapterName)) {
				log.debug(getLogPrefix()+"setAdapters() addding adapter ["+adapterName+"] to filter");
				AdapterFilter af=new AdapterFilter();
				af.setAdapter(adapterName);
				registerAdapterFilter(af);
			}
		}
	}
	/**
	 * get List of all adapters that are present in the FilterMap.
	 */
	public String[] getAdapters() {
		String[] result=(String[])adapterFilters.keySet().toArray(new String[adapterFilters.size()]);
		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix()+"getAdapters() returns results:");
			for (int i=0; i<result.length; i++) {
				log.debug(getLogPrefix()+"getAdapters() returns ["+ result[i]+"]");
			}
		}
		return result;
	}

	public List<IAdapter> getAdapterList() {
		List<IAdapter> result=new LinkedList<IAdapter>();
		MonitorManager mm=MonitorManager.getInstance();
		for (Iterator<String> it=adapterFilters.keySet(). iterator(); it.hasNext();) {
			String adapterName=(String)it.next();
			IAdapter adapter=mm.findAdapterByName(adapterName);
			if (adapter!=null) {
				result.add(adapter);
				if (log.isDebugEnabled()) {
					log.debug(getLogPrefix()+"getAdapterList() returns adapter ["+adapterName+"]");
				}
			} else {
				log.warn(getLogPrefix()+"getAdapterList() cannot find adapter ["+adapterName+"]");
			}
		}
		return result;
	}

	/**
	 * set List of all throwers that can trigger this Trigger.
	 */
	public void setSources(String[] sourcesArr) {
		log.debug(getLogPrefix()+"setSources()");
		List<EventThrowing> list=MonitorManager.getInstance().getEventSources((List<String>)null);
		log.debug(getLogPrefix()+"setSources() clearing adapterFilter");
		adapterFilters.clear();
		for (int i=0; i<list.size();i++) {
			EventThrowing thrower=(EventThrowing)list.get(i);
			IAdapter adapter = thrower.getAdapter();
			String adaptername;
			String sourcename;
			if (adapter==null) {
				adaptername="-";
			} else {
				adaptername=adapter.getName();
			}
			sourcename=adaptername+" / "+thrower.getEventSourceName();
			//log.debug("setSources() checking for source ["+sourcename+"]");
			for (int j=0; j<sourcesArr.length; j++) {
				if (sourcesArr[j].equals(sourcename)) {
					AdapterFilter af =(AdapterFilter)adapterFilters.get(adaptername);
					if (af==null) {
						af = new AdapterFilter();
						af.setAdapter(adaptername);
						log.debug(getLogPrefix()+"setSources() registered adapter ["+adaptername+"]");
						registerAdapterFilter(af);
					}
					af.registerSubOject(thrower.getEventSourceName());
					log.debug(getLogPrefix()+"setSources() registered source ["+thrower.getEventSourceName()+"] on adapter ["+adapter.getName()+"]");
					break;
				}
			}
		}
	}
	
	/**
	 * get List of all throwers that can trigger this Trigger.
	 */
	public String[] getSources() {
		List<String> list=new ArrayList<String>();
		for(Iterator adapterIterator=adapterFilters.entrySet().iterator();adapterIterator.hasNext();) {
			Map.Entry entry= (Map.Entry)adapterIterator.next();
			String adapterName=(String)entry.getKey();
			AdapterFilter af=(AdapterFilter)entry.getValue();
			for(Iterator subSourceIterator=af.getSubObjectList().iterator();subSourceIterator.hasNext();) {
				String throwerName=(String)subSourceIterator.next();
				String sourceName=adapterName+" / "+throwerName;
				log.debug(getLogPrefix()+"getSources() adding returned source ["+sourceName+"]");
				list.add(sourceName);
			}
		}
		String[] result=new String[list.size()];
		return result=(String[])list.toArray(result);
	}


	public List<EventThrowing> getSourceList() {
		if (log.isDebugEnabled()) log.debug(getLogPrefix()+"getSourceList() collecting sources:");
		List<EventThrowing> list=new ArrayList<EventThrowing>();
		MonitorManager mm = MonitorManager.getInstance();
		for(Iterator adapterIterator=adapterFilters.entrySet().iterator();adapterIterator.hasNext();) {
			Map.Entry entry= (Map.Entry)adapterIterator.next();
			String adapterName=(String)entry.getKey();
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"getSourceList() collecting sources for adapter ["+adapterName+"]:");
			AdapterFilter af=(AdapterFilter)entry.getValue();
			for(Iterator subSourceIterator=af.getSubObjectList().iterator();subSourceIterator.hasNext();) {
				String throwerName=(String)subSourceIterator.next();
				EventThrowing thrower=mm.findThrowerByName(adapterName,throwerName);
				if (log.isDebugEnabled()) log.debug(getLogPrefix()+"getSourceList() adding source adapter ["+adapterName+"], source ["+thrower.getEventSourceName()+"]");
				list.add(thrower);
			}
		}
		return list;
	}


	public void setSeverity(String severity) {
		setSeverityEnum((SeverityEnum)SeverityEnum.getEnumMap().get(severity));
	}
	public void setSeverityEnum(SeverityEnum enumeration) {
		severity = enumeration;
	}
	public SeverityEnum getSeverityEnum() {
		return severity;
	}
	public String getSeverity() {
		return severity==null?null:severity.getName();
	}

	public void setThreshold(int i) {
		threshold = i;
	}
	public int getThreshold() {
		return threshold;
	}

	public void setPeriod(int i) {
		period = i;
	}
	public int getPeriod() {
		return period;
	}

	public Map<String, AdapterFilter> getAdapterFilters() {
		return adapterFilters;
	}

	public void registerAdapterFilter(AdapterFilter af) {
		adapterFilters.put(af.getAdapter(),af);
		if (getSourceFiltering()==SOURCE_FILTERING_NONE) {
			setSourceFiltering(SOURCE_FILTERING_BY_ADAPTER);
		}
	}


	public void setFilterExclusive(boolean b) {
		filterExclusive = b;
	}
	public boolean isFilterExclusive() {
		return filterExclusive;
	}

	public void setFilteringToLowerLevelObjects(Object dummy) {
		setSourceFiltering(SOURCE_FILTERING_BY_LOWER_LEVEL_OBJECT);
	}

	public boolean isFilterOnLowerLevelObjects() {
		return sourceFiltering==SOURCE_FILTERING_BY_LOWER_LEVEL_OBJECT;
	}
	public boolean isFilterOnAdapters() {
		return sourceFiltering==SOURCE_FILTERING_BY_ADAPTER;
	}

	public void setSourceFiltering(int i) {
		sourceFiltering = i;
	}
	public int getSourceFiltering() {
		return sourceFiltering;
	}

}
