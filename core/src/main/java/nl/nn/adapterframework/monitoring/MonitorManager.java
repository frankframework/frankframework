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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.nn.adapterframework.configuration.AttributeCheckingRule;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.Lock;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.digester.AbstractObjectCreationFactory;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.logging.log4j.Logger;
import org.xml.sax.Attributes;

/**
 * Manager for Monitoring.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class MonitorManager implements EventHandler {
	protected Logger log = LogUtil.getLogger(this);

	private Configuration configuration;
	private List<Monitor> monitors = new ArrayList<Monitor>();				// all monitors managed by this monitormanager
	private Map<String, IMonitorAdapter> destinations = new LinkedHashMap<String, IMonitorAdapter>();	// all destinations (that can receive status messages) managed by this monitormanager

	private Map eventNotificationListeners = new LinkedHashMap(); // map by event of triggers that need to be notified of occurrence of event.


	private List<EventThrowing> eventThrowers = new ArrayList<EventThrowing>();			// static list of all throwers of events;

	private Map eventsByThrower = new LinkedHashMap();
	private Map eventsByThrowerType = new LinkedHashMap();
	private Map throwersByEvent = new LinkedHashMap();
	private Map throwerTypesByEvent = new LinkedHashMap();

	private Map throwersByAdapter = new LinkedHashMap();
	private Map adaptersByThrowers = new LinkedHashMap();
	private Map eventsByAdapter = new LinkedHashMap();
	private Map adaptersByEvent = new LinkedHashMap();



	private boolean enabled = AppConstants.getInstance().getBoolean("monitoring.enabled", false);
	private Date lastStateChange=null;

	private Lock structureLock = new Lock();

	private static MonitorManager self=null;

	public static final boolean traceReconfigure=false;



	private MonitorManager() {
	}

	public void configure(Configuration configuration) throws ConfigurationException {
		Collections.sort(eventThrowers,new EventThrowerComparator());
		this.configuration=configuration;
		reconfigure();
	}

	/*
	 * reconfigure all destinations and all monitors.
	 * monitors will register all required eventNotificationListeners.
	 */
	public void reconfigure() throws ConfigurationException {
		if (traceReconfigure && log.isDebugEnabled()) log.debug("reconfigure() clearing eventNotificationListeners");
		eventNotificationListeners.clear();
		if (traceReconfigure && log.isDebugEnabled()) log.debug("reconfigure() configuring destinations");
		for (Iterator<String> it=destinations.keySet().iterator(); it.hasNext();) {
			String name=(String)it.next();
			IMonitorAdapter destination = getDestination(name);
			destination.configure();
		}
		if (traceReconfigure && log.isDebugEnabled()) log.debug("reconfigure() configuring monitors");
		for (Iterator it=monitors.iterator(); it.hasNext();) {
			Monitor monitor = (Monitor)it.next();
			monitor.configure();
		}
	}

	private class EventThrowerComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			EventThrowing et1=(EventThrowing)o1;
			EventThrowing et2=(EventThrowing)o2;

			int comp1=et1.getAdapter().getName().compareTo(et2.getAdapter().getName());
			if (comp1!=0) {
				return comp1;
			}
			return et1.getEventSourceName().compareTo(et2.getEventSourceName());
		}
	}

	public void registerStateChange(Date date) {
		lastStateChange=date;
	}

	public void updateDestinations(String[] selectedDestinations) {
		Map monitorDestinations=new HashMap();
		log.debug("setting destinations selectedDestinations.length ["+selectedDestinations.length+"]");
		for (int i=0; i<selectedDestinations.length; i++) {
			String curSelectedDestination=selectedDestinations[i];
			log.debug("processing destination["+curSelectedDestination+"]");
			int pos=curSelectedDestination.indexOf(',');
			log.debug("pos["+pos+"]");
			int index=Integer.parseInt(curSelectedDestination.substring(0,pos));
			log.debug("index["+index+"]");
			Monitor monitor=getMonitor(index);
			String destination=curSelectedDestination.substring(pos+1);
			log.debug("destination["+destination+"]");
			Set monitorDestinationSet=(Set)monitorDestinations.get(monitor);
			if (monitorDestinationSet==null) {
				monitorDestinationSet=new HashSet();
				monitorDestinations.put(monitor,monitorDestinationSet);
			}
			monitorDestinationSet.add(destination);
		}
		for(int i=0;i<getMonitors().size();i++){
			Monitor monitor=getMonitor(i);
			Set monitorDestinationSet=(Set)monitorDestinations.get(monitor);
//			log.debug("setting ["+monitorDestinationSet.size()+"] destinations for monitor ["+monitor.getName()+"]");
			monitor.setDestinationSet(monitorDestinationSet);
		}
	}

	public class CreationFactory extends AbstractObjectCreationFactory {

		public CreationFactory() {
			super();
		}

		public Object createObject(Attributes attributes) throws Exception {
			return getInstance();
		}
	}

	private class DestinationCleanup extends Rule {

		public void begin(String uri, String elementName, Attributes attributes) throws Exception {
			destinations.clear();
		}

	}

	public void setDigesterRules(Digester d) {
		Rule attributeChecker=new AttributeCheckingRule();

		d.addFactoryCreate("*/monitoring",new CreationFactory());
		d.addSetProperties("*/monitoring");
		d.addSetTop("*/monitoring","register");
		d.addRule("*/monitoring", attributeChecker);

		d.addRule("*/monitoring/destinations",new DestinationCleanup());

		d.addObjectCreate("*/destination","className",IMonitorAdapter.class);
		d.addSetProperties("*/destination");
		d.addSetTop("*/destination","register");
		d.addRule("*/destination", attributeChecker);

		d.addObjectCreate("*/destination/sender","className",ISender.class);
		d.addSetProperties("*/destination/sender");
		d.addSetNext("*/destination/sender","setSender");
		d.addRule("*/destination/sender", attributeChecker);

		d.addObjectCreate("*/monitor",Monitor.class);
		d.addSetProperties("*/monitor");
		d.addSetTop("*/monitor","register");
		d.addRule("*/monitor", attributeChecker);

		d.addObjectCreate("*/alarm",Trigger.class);
		d.addSetProperties("*/alarm");
		d.addSetNext("*/alarm","registerAlarm");
		d.addRule("*/alarm", attributeChecker);

		d.addCallMethod("*/alarm/events/event", "addEventCode", 0);


		d.addObjectCreate("*/clearing",Trigger.class);
		d.addSetProperties("*/clearing");
		d.addSetNext("*/clearing","registerClearing");
		d.addRule("*/clearing", attributeChecker);

		d.addCallMethod("*/clearing/events/event", "addEventCode", 0);

		d.addObjectCreate("*/trigger",Trigger.class);
		d.addSetProperties("*/trigger");
		d.addSetNext("*/trigger","registerTrigger");
		d.addRule("*/trigger", attributeChecker);

		d.addCallMethod("*/trigger/events/event", "addEventCode", 0);

		d.addObjectCreate("*/adapterfilter",AdapterFilter.class);
		d.addSetProperties("*/adapterfilter");
		d.addSetNext("*/adapterfilter","registerAdapterFilter");
		d.addRule("*/adapterfilter", attributeChecker);

		d.addSetNext("*/adapterfilter/sources","setFilteringToLowerLevelObjects");
		d.addCallMethod("*/adapterfilter/sources/source", "registerSubOject", 0);

	}

	public void register(Object dummy) {
		// do nothing, just to get rid of stack item
	}

	public void registerDestination(IMonitorAdapter monitorAdapter) {
		destinations.put(monitorAdapter.getName(),monitorAdapter);
	}
	public IMonitorAdapter getDestination(String name) {
		return (IMonitorAdapter)destinations.get(name);
	}
	public Map<String, IMonitorAdapter> getDestinations() {
		return destinations;
	}

	public synchronized static MonitorManager getInstance() {
		if (self==null) {
			self=new MonitorManager();
		}
		return self;
	}

	public static EventHandler getEventHandler() {
		if (getInstance().isEnabled()) {
			return getInstance();
		} else {
			return null;
		}
	}


	public void addMonitor(Monitor monitor) {
		monitor.setOwner(this);
		monitors.add(monitor);
	}
	public Monitor removeMonitor(int index) {
		Monitor result=null;
		result = (Monitor)monitors.remove(index);
		return result;
	}
	public Monitor getMonitor(int index) {
		return (Monitor)monitors.get(index);
	}
	public Monitor findMonitor(String name) {
		for (int i=0; i<monitors.size(); i++) {
			Monitor monitor = (Monitor)monitors.get(i);
			if (name!=null && name.equals(monitor.getName()) || name==monitor.getName()) {
				return monitor;
			}
		}
		return null;
	}

	public List<Monitor> getMonitors() {
		return monitors;
	}

	public IAdapter findAdapterByName(String adapterName) {
		return configuration.getRegisteredAdapter(adapterName);
	}

	/**
	 * Returns a list of eventcodes that each can be thrown by at least one of the throwers in the list.
	 * Used by EditTrigger to populate events-listbox.
	 */
	public List getEventCodesBySources(List<EventThrowing> throwers) {
		if (log.isDebugEnabled()) {
			log.debug("getEventCodesBySources() throwers:");
			for (Iterator<EventThrowing> it=throwers.iterator(); it.hasNext();) {
				EventThrowing thrower=(EventThrowing)it.next();
				log.debug("getEventCodesBySources() thrower ["+thrower.getEventSourceName()+"]");
			}
		}
		if (throwers!=null) {
			List result = new ArrayList();
			if (throwers.size()==0) {
				return result;
			}
			result.addAll((List)eventsByThrower.get(throwers.get(0)));
			for (int i=1; i<throwers.size(); i++) {
				List extraEvents = (List)eventsByThrower.get(throwers.get(i));
				for (Iterator it=extraEvents.iterator(); it.hasNext();) {
					String eventCode=(String)it.next();
					if (!result.contains(eventCode)) {
						result.add(eventCode);
					}
				}
			}
			return result;
		} else {
			List result = new ArrayList();
			for (Iterator it=throwersByEvent.keySet().iterator(); it.hasNext();) {
				result.add(it.next());
			}
			return result;
		}
	}


//	/**
//	 * Returns a list of eventcodes that can be thrown by this thrower
//	 * @param thrower
//	 * @return
//	 */
//	public List getEventCodes(EventThrowing thrower) {
//		if (thrower!=null) {
//			return (List)eventsByThrower.get(thrower);
//		} else {
//			List result = new ArrayList();
//			for (Iterator it=throwersByEvent.keySet().iterator(); it.hasNext();) {
//				result.add(it.next());
//			}
//			return result;
//		}
//	}

	/**
	 * Returns a list of eventcodes that can be thrown by at least one of the adapters in the list.
	 * Used by EditTrigger to populate events-listbox.
	 */
	public List getEventCodesByAdapters(String[] adapters) {
		List result = new ArrayList();
		for (int j=0; j<adapters.length; j++) {
			IAdapter adapter=findAdapterByName(adapters[j]);
			if (adapter!=null) {
				List events=(List)eventsByAdapter.get(adapter);
				if (events!=null) {
					for (int i=0; i<events.size(); i++) {
						String event=(String)events.get(i);
						if (!result.contains(event)) {
							result.add(event);
						}
					}
				}
			}
		}
		return result;
	}


//	/**
//	 * Returns a list of throwers that can throw this event.
//	 */
//	public List getEventSources(String eventCode) {
//		if (eventCode!=null) {
//			return (List)throwersByEvent.get(eventCode);
//		} else {
//			List result = new ArrayList();
//			for (Iterator it=eventsByThrower.keySet().iterator(); it.hasNext();) {
//				result.add(it.next());
//			}
//			return result;
//		}
//	}
	/**
	 * Returns a list of throwers that can throw at least one of the events in the list.
	 */
	public List<EventThrowing> getEventSources(List<String> eventCodes) {
		if (eventCodes!=null) {
			if (eventCodes.size()==0) {
				return new ArrayList();
			} else {
				List<EventThrowing> result=new LinkedList<EventThrowing>();
				for (Iterator sit=eventsByThrower.keySet().iterator(); sit.hasNext();) {
					EventThrowing thrower=(EventThrowing)sit.next();
					log.debug("getEventSources() checks if thrower ["+thrower.getEventSourceName()+"] can throw one of the specified events");
					List eventsOfThrower = (List)eventsByThrower.get(thrower);
					boolean foundEvent=false;
					for (Iterator<String> eit=eventCodes.iterator(); !foundEvent && eit.hasNext();) {
						String eventCode=(String)eit.next();
						log.debug("getEventSources() checks if thrower ["+thrower.getEventSourceName()+"] can throw event ["+eventCode+"]");

						foundEvent=eventsOfThrower.contains(eventCode);
					}
					if (foundEvent) {
						log.debug("getEventSources() adds ["+thrower.getEventSourceName()+"] to resultset");
						result.add(thrower);
					}
				}
				return result;
			}
		} else {
			List result = new ArrayList();
			for (Iterator it=eventsByThrower.keySet().iterator(); it.hasNext();) {
				result.add(it.next());
			}
			return result;
		}
	}
	/**
	 * Returns a list of names of throwers that can throw at least one of the events in the list.
	 * Used by EditTrigger to populate sources-listbox.
	 */
	public List<String> getEventSourceNamesByEventCodes(List<String> eventCodes) {
		List<String> result = new ArrayList<String>();
		List<EventThrowing> sources=getEventSources(eventCodes);
		if (sources!=null) {
			for (int i=0; i<sources.size(); i++) {
				EventThrowing source=(EventThrowing)sources.get(i);
				String adapterName=source.getAdapter()!=null?source.getAdapter().getName():"";
				String eventSourceName=adapterName+" / "+source.getEventSourceName();
				if (log.isDebugEnabled()) log.debug("getEventSourceNamesByEventCodes adding ["+eventSourceName+"]");
				result.add(eventSourceName);
			}
		}
		Collections.sort(result);
		return result;
	}
	/**
	 * Returns a list of names of throwers that can throw at least one of the events in the list.
	 * Used by EditTrigger to populate sources-listbox.
	 */
	public List getEventSourceNamesByAdapters(String[] adapters) {
		List result = new ArrayList();
		for (int j=0; j<adapters.length; j++) {
			IAdapter adapter=findAdapterByName(adapters[j]);
			if (adapter!=null) {
				List sources=(List)throwersByAdapter.get(adapter);
				if (sources!=null) {
					for (int i=0; i<sources.size(); i++) {
						EventThrowing source=(EventThrowing)sources.get(i);
						String adapterName=source.getAdapter()!=null?source.getAdapter().getName():"";
						result.add(adapterName+" / "+source.getEventSourceName());
					}
				}
			}
		}
		Collections.sort(result);
		return result;
	}


	public List getAdapterNames() {
		List result = new ArrayList();
		for (Iterator it=eventsByAdapter.keySet().iterator();it.hasNext();) {
			IAdapter adapter=(IAdapter)it.next();
			result.add(adapter.getName());
		}
		Collections.sort(result);
		return result;
	}
	public List getAdapterNamesByEventCodes(List eventCodes) {
		List result = new ArrayList();
		for (int i=0; i<eventCodes.size(); i++) {
			String eventCode=(String)eventCodes.get(i);
			List adapters=(List)adaptersByEvent.get(eventCode);
			for (Iterator it=adapters.iterator();it.hasNext();) {
				IAdapter adapter=(IAdapter)it.next();
				if (!result.contains(adapter.getName())) {
					result.add(adapter.getName());
				}
			}
		}
		Collections.sort(result);
		return result;
	}
	public List getAdapterNamesBySources(List throwers) {
		List result = new ArrayList();
		for (int i=0; i<throwers.size(); i++) {
			EventThrowing thrower=(EventThrowing)throwers.get(i);
			List adapters=(List)adaptersByThrowers.get(thrower);
			for (Iterator it=adapters.iterator();it.hasNext();) {
				IAdapter adapter=(IAdapter)it.next();
				if (!result.contains(adapter.getName())) {
					result.add(adapter.getName());
				}
			}
		}
		Collections.sort(result);
		return result;
	}




	private void addItemToMapOfLists(Map listMap, Object item, Object key, String mapname) {
		if (item==null || key==null) {
			log.warn("addItemToMapOfLists() null item or key in map ["+mapname+"] key ["+key+"] item ["+item+"]");
		}
		List subList = (List)listMap.get(key);
		if (subList==null) {
			subList = new ArrayList();
			listMap.put(key,subList);
		}
		if (!subList.contains(item)) {
			subList.add(item);
		}
	}

	/**
	 * any object in the configuration has to call this function at configuration
	 * time to notifiy the monitoring system of any event that he may wish to throw.
	 */
	public void registerEvent(EventThrowing thrower, String eventCode) {

		if (log.isDebugEnabled()) {
            log.debug("registerEvent [" + eventCode + "] for adapter [" + (thrower.getAdapter() == null ? null : thrower.getAdapter().getName()) + "] object [" + thrower.getEventSourceName() + "]");
        }

		addItemToMapOfLists(eventsByThrower, eventCode, thrower, "eventsByThrower");
		addItemToMapOfLists(throwersByEvent, thrower, eventCode, "throwersByEvent");
		addItemToMapOfLists(throwerTypesByEvent, thrower.getClass(), eventCode, "throwerTypesByEvent");
		addItemToMapOfLists(eventsByThrowerType, eventCode, thrower.getClass(), "eventsByThrowerType");

		addItemToMapOfLists(throwersByAdapter, thrower, thrower.getAdapter(), "throwersByAdapter");
		addItemToMapOfLists(adaptersByThrowers, thrower.getAdapter(), thrower, "adaptersByThrowers");
		addItemToMapOfLists(eventsByAdapter, eventCode, thrower.getAdapter(), "eventsByAdapter");
		addItemToMapOfLists(adaptersByEvent, thrower.getAdapter(), eventCode, "adaptersByEvent");

		if (!eventThrowers.contains(thrower)) {
			eventThrowers.add(thrower);
		}

	}

	private class EventSourceIterator implements Iterator {

		List eventCodes;
		Map adapterFilters;
		boolean filterOnLowerLevelObjects;
		boolean filterInclusive;

		Iterator sourceIterator=null;

		private EventThrowing currentEventSource=null;
		private boolean nextCalled=false;

		public EventSourceIterator(List eventCodes, Map adapterFilters, boolean filterOnLowerLevelObjects, boolean filterInclusive) {
			this.eventCodes=eventCodes;
			this.adapterFilters=adapterFilters;
			this.filterOnLowerLevelObjects=filterOnLowerLevelObjects;
			this.filterInclusive=filterInclusive;

			sourceIterator=eventThrowers.iterator();
		}

		private void determineNextEventSource() {
			while (sourceIterator.hasNext()) {
				currentEventSource = (EventThrowing)sourceIterator.next();
				IAdapter adapter=currentEventSource.getAdapter();
				if (adapterFilters!=null) {
					if (!(filterOnLowerLevelObjects && !filterInclusive ||
					  	   adapterFilters.containsKey(adapter)==filterInclusive
					  	 )
				       ) {
				    	// this adapter will not contain objects to be evaluated
				    	continue;
					}
					if (filterOnLowerLevelObjects) {
						// construct from the filter a list of throwers to match against potential throwers
						List throwerFilter=(List)adapterFilters.get(adapter);
						if (throwerFilter!=null &&
						    (throwerFilter.contains(currentEventSource.getEventSourceName())!=filterInclusive)
						   ) {
						   	// this source is either excluded or not included by the filter
						   	continue;
						}
					}
				}
				// when we are here, the currentEventSource has passed the adapterFilter
				if (eventCodes==null) {
					return;
				}
				List throwersEvents=(List)eventsByThrower.get(currentEventSource);
				for (Iterator eventIterator=eventCodes.iterator(); eventIterator.hasNext();) {
					String eventCode=(String)eventIterator.next();
					if (throwersEvents.contains(eventCode)) {
						return;
					}
				}
			}
			currentEventSource=null;
		}

		public boolean hasNext() {
			if (nextCalled) {
				determineNextEventSource();
				nextCalled=false;
			}
			return currentEventSource!=null;
		}

		public Object next() {
			if (nextCalled) {
				determineNextEventSource();
			}
			nextCalled=true;
			return currentEventSource;
		}

		public void remove() {
			// will not be used...
		}
	}

	/**
	 * Provides a list of items of type EventThrowing.
	 */
	public List filterEventSources(List eventCodes, Map adapterFilters, boolean filterOnLowerLevelObjects, boolean filterInclusive) {
		List result = new LinkedList();
		for (Iterator it = new EventSourceIterator(eventCodes, adapterFilters, filterOnLowerLevelObjects, filterInclusive); it.hasNext();) {
			EventThrowing thrower = (EventThrowing)it.next();
			result.add(thrower);
		}
		return result;
	}

	public void registerEventNotificationListener(Trigger trigger, List eventCodes, Map adapterFilters, boolean filterOnLowerLevelObjects, boolean filterExclusive) throws MonitorException {
		boolean performFiltering=adapterFilters!=null;
		for (Iterator eventIt=eventCodes.iterator(); eventIt.hasNext();) {
			String eventCode=(String)eventIt.next();

			if (traceReconfigure && log.isDebugEnabled()) log.debug("registerEventNotificationListener1() for event ["+eventCode+"]");
			List eventAdapterList = (List)adaptersByEvent.get(eventCode);
			if (eventAdapterList==null) {
				log.warn("registerEventNotificationListener1() event ["+eventCode+"] is not registered by any adapter, ignoring...");
			} else {
				List eventThrowersList= (List)throwersByEvent.get(eventCode);
				for (Iterator adapterIt=eventAdapterList.iterator();adapterIt.hasNext();) {
					IAdapter adapter=(IAdapter)adapterIt.next();
					String adaptername=adapter.getName();
					AdapterFilter adapterFilter=null;
					boolean adapterInFilter=false;
					if (performFiltering) {
						adapterFilter=(AdapterFilter)adapterFilters.get(adaptername);
						adapterInFilter=(adapterFilter!=null);
					}
					boolean adapterNeedsRegistering=
						!performFiltering || 							// if not filtered, always register
						(adapterInFilter != filterExclusive) || 		// register all that pass on the adapterlevel
						(filterOnLowerLevelObjects && adapterInFilter);	// further evaluation required for lower levels
					if (traceReconfigure && log.isDebugEnabled()) log.debug("registerEventNotificationListener1() for event ["+eventCode+"] adapter ["+adaptername+"] adapterNeedsRegistering ["+adapterNeedsRegistering+"]");
					if (adapterNeedsRegistering) {
						// when we are here, some (or all of) adapter's objects must be registered
						List adapterThrowerList = (List)throwersByAdapter.get(adapter);
						for (Iterator throwerIt=adapterThrowerList.iterator(); throwerIt.hasNext(); ) {
							EventThrowing candidate = (EventThrowing)throwerIt.next();
							if (eventThrowersList.contains(candidate)) {
								// this subelement of the adapter can throw the event that we are looking for
								// now do final evaluation.
								// filtering out can only be on lower level
								boolean candiateMustBeRegistered=
									!performFiltering || !filterOnLowerLevelObjects || !adapterInFilter || 	 // otherwise registriation allways required
									(adapterFilter.getSubObjectList().contains(candidate.getEventSourceName())!= filterExclusive);
								if (traceReconfigure && log.isDebugEnabled()) log.debug("registerEventNotificationListener1() for event ["+eventCode+"] adapter ["+adaptername+"] candidate ["+candidate.getEventSourceName()+"] candiateMustBeRegistered ["+candiateMustBeRegistered+"]");

								if (candiateMustBeRegistered) {
									registerEventNotificationListener(trigger,eventCode,candidate);
								}
							}
						}

					}
				}
			}
		}
	}

	// structure of eventNotificationListeners
	// eventNotificationListeners = map by eventcode of map of notification listeners

	// eventNotificationListeners = map(eventcode,map(trigger,throwerset or null))


	public void registerEventNotificationListener(Trigger trigger, String eventCode, EventThrowing thrower) throws MonitorException {
		if (traceReconfigure && log.isDebugEnabled()) {
			String adapterName=thrower.getAdapter()==null?"<none>":thrower.getAdapter().getName();
			log.debug("registerEventNotificationListener2() eventCode ["+eventCode+"] adapter ["+adapterName+"] thrower ["+thrower.getEventSourceName()+"]");
		}

		Map notificationListenersOfEvent = (Map)eventNotificationListeners.get(eventCode);
		if (notificationListenersOfEvent==null) {
			notificationListenersOfEvent=new LinkedHashMap();
			eventNotificationListeners.put(eventCode,notificationListenersOfEvent);
//			if (traceReconfigure && log.isDebugEnabled()) log.debug("registerEventNotificationListener eventCode ["+eventCode+"] created map of notificationlisteners");
		}

		// now notificationListenersOfEvent=map(trigger,throwerset or null)
		// if mapentry is null, all throwers will cause triggering.
		// otherwise mapentry is a list of throwers that will cause triggering

		boolean triggerRegistered=notificationListenersOfEvent.containsKey(trigger);
		Set throwerFilter = (Set)notificationListenersOfEvent.get(trigger);
		if (triggerRegistered) {
			if (throwerFilter==null) {
				throw new MonitorException("unfiltered event notification for Trigger ["+trigger+"] eventcode ["+eventCode+"] already registered");
			} else {
				if (thrower==null) {
					throw new MonitorException("Cannot register event notification for Trigger ["+trigger+"] eventcode ["+eventCode+"] both generic and for specific thrower ["+thrower.getEventSourceName()+"]");
				}
			}
		}
		if (thrower!=null) {
			if (throwerFilter==null) {
				throwerFilter=new HashSet();
				notificationListenersOfEvent.put(trigger,throwerFilter);
			}
			throwerFilter.add(thrower);
//			if (traceReconfigure && log.isDebugEnabled()) {
//				String adapterName=thrower.getAdapter()==null?"<none>":thrower.getAdapter().getName();
//				log.debug("registerEventNotificationListener eventCode ["+eventCode+"] adapter ["+adapterName+"] thrower ["+thrower.getEventSourceName()+"] added to throwerfilter");
//		    }

		} else {
			notificationListenersOfEvent.put(trigger,null);
//			if (traceReconfigure && log.isDebugEnabled()) {
//				String adapterName=thrower.getAdapter()==null?"<none>":thrower.getAdapter().getName();
//				log.debug("registerEventNotificationListener eventCode ["+eventCode+"] adapter ["+adapterName+"] thrower ["+thrower.getEventSourceName()+"] added pass-all throwerfilter");
//			}
		}
	}

	public void fireEvent(EventThrowing source, String eventCode) {
		if (isEnabled()) {
			try {
				structureLock.acquireShared();
				try {
					Map notificationListenersOfEvent = (Map)eventNotificationListeners.get(eventCode);
					if (notificationListenersOfEvent!=null) {
						for (Iterator it=notificationListenersOfEvent.keySet().iterator(); it.hasNext();) {
							Trigger trigger = (Trigger)it.next();
							Set throwerFilter = (Set)notificationListenersOfEvent.get(trigger);
							if (throwerFilter==null || throwerFilter.contains(source)) {
								try {
									trigger.evaluateEvent(source,eventCode);
								} catch (MonitorException e) {
									log.error("Could not evaluate event ["+eventCode+"]",e);
								}
							}
						}
					}
				} finally {
					structureLock.releaseShared();
				}
			} catch (InterruptedException e) {
				log.error("Could not obtain lock for fireEvent" , e);
			}
		}
	}

	public XmlBuilder getStatusXml() {

		long freeMem = Runtime.getRuntime().freeMemory();
		long totalMem = Runtime.getRuntime().totalMemory();

		XmlBuilder statusXml=new XmlBuilder("monitorstatus");
		if (lastStateChange!=null) {
			statusXml.addAttribute("lastStateChange",DateUtils.format(lastStateChange,DateUtils.FORMAT_FULL_GENERIC));
		}
		statusXml.addAttribute("timestamp",DateUtils.format(new Date()));
		statusXml.addAttribute("heapSize", Long.toString (totalMem-freeMem) );
		statusXml.addAttribute("totalMemory", Long.toString(totalMem) );

		for (int i=0; i<monitors.size(); i++) {
			Monitor monitor=getMonitor(i);
			statusXml.addSubElement(monitor.getStatusXml());
		}
		return statusXml;
	}

	public XmlBuilder toXml() {
		XmlBuilder configXml=new XmlBuilder("monitoring");
		configXml.addAttribute("enabled",isEnabled());
		XmlBuilder destinationsXml=new XmlBuilder("destinations");
		for (Iterator it=destinations.keySet().iterator(); it.hasNext(); ) {
			String name=(String)it.next();
			IMonitorAdapter ma=getDestination(name);

			XmlBuilder destinationXml=new XmlBuilder("destination");
			destinationXml.addAttribute("name",ma.getName());
			destinationXml.addAttribute("className",ma.getClass().getName());

			destinationsXml.addSubElement(ma.toXml());
		}
		configXml.addSubElement(destinationsXml);
		XmlBuilder monitorsXml=new XmlBuilder("monitors");
		for (int i=0; i<monitors.size(); i++) {
			Monitor monitor=getMonitor(i);
			monitorsXml.addSubElement(monitor.toXml());
		}
		configXml.addSubElement(monitorsXml);

		return configXml;
	}

	public List getThrowers() {
		List result = new ArrayList(eventsByThrower.keySet());
		return result;
	}
	public Iterator getThrowerIterator() {
		return eventsByThrower.keySet().iterator();
	}

	public List getAdapters() {
		List result = new ArrayList(eventsByAdapter.keySet());
		return result;
	}



	public EventThrowing findThrowerByName(String adapterName, String thrower) {
		for(Iterator it=getThrowerIterator(); it.hasNext();) {
			EventThrowing candidate = (EventThrowing)it.next();
			if (candidate.getAdapter().getName().equals(adapterName) && candidate.getEventSourceName().equals(thrower)) {
				return candidate;
			}
		}
		return null;
	}

	public Lock getStructureLock() {
		return structureLock;
	}

	public void setEnabled(boolean b) {
		enabled = b;
	}
	public boolean isEnabled() {
		return enabled;
	}

	public Map getThrowersByEvent() {
		return throwersByEvent;
	}
	public Map getThrowerTypesByEvent() {
		return throwerTypesByEvent;
	}

	public Map getEventsByThrowerType() {
		return eventsByThrowerType;
	}

}
