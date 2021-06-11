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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.lifecycle.ConfigurableLifecyleBase;
import nl.nn.adapterframework.monitoring.events.RegisterMonitorEvent;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Manager for Monitoring.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class MonitorManager extends ConfigurableLifecyleBase implements ApplicationContextAware, ApplicationListener<RegisterMonitorEvent> {
	protected Logger log = LogUtil.getLogger(this);

	private @Getter @Setter ApplicationContext applicationContext;
	private List<Monitor> monitors = new ArrayList<>();				// all monitors managed by this monitormanager
	private Map<String, IMonitorAdapter> destinations = new LinkedHashMap<String, IMonitorAdapter>();	// all destinations (that can receive status messages) managed by this monitormanager

	private boolean enabled = AppConstants.getInstance().getBoolean("monitoring.enabled", false);
	private Date lastStateChange=null;

	@Override
	public void configure() {
		try {
			reconfigure();
		} catch (ConfigurationException e) {
			// TODO log these, or config warnings?
			e.printStackTrace();
		}
	}

	/*
	 * reconfigure all destinations and all monitors.
	 * monitors will register all required eventNotificationListeners.
	 */
	public void reconfigure() throws ConfigurationException {
		if (log.isDebugEnabled()) log.debug("reconfigure() configuring destinations");
		for (Iterator<String> it=destinations.keySet().iterator(); it.hasNext();) {
			String name=(String)it.next();
			IMonitorAdapter destination = getDestination(name);
			destination.configure();
		}
		if (log.isDebugEnabled()) log.debug("reconfigure() configuring monitors");
		for(Monitor monitor : monitors) {
			monitor.configure();
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

	public void registerDestination(IMonitorAdapter monitorAdapter) {
		destinations.put(monitorAdapter.getName(),monitorAdapter);
	}
	public IMonitorAdapter getDestination(String name) {
		return destinations.get(name);
	}
	public Map<String, IMonitorAdapter> getDestinations() {
		return destinations;
	}

	/**
	 * Helper method to retrieve the MonitorManager from the ApplicationContext
	 */
	public static MonitorManager getInstance(ApplicationContext applicationContext) {
		return applicationContext.getBean("monitorManager", MonitorManager.class);
	}

	@Override
	public void onApplicationEvent(RegisterMonitorEvent event) {
		EventThrowing thrower = event.getSource();
		String eventCode = event.getEventCode();

		if (log.isDebugEnabled()) {
			log.debug("registerEvent ["+eventCode+"] for adapter ["+(thrower.getAdapter() == null ? null : thrower.getAdapter().getName())+"] object ["+thrower.getEventSourceName()+"]");
		}

		register(thrower, eventCode);
	}

	public void addMonitor(Monitor monitor) {
		monitor.setManager(this);
		monitors.add(monitor);
	}

	public void removeMonitor(Monitor monitor) {
		int index = monitors.indexOf(monitor);
		if(index > -1) {
			AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
			factory.destroyBean(monitor);
			monitors.remove(index);
		}
	}

	public Monitor removeMonitor(int index) {
		Monitor result=null;
		result = monitors.remove(index);
		return result;
	}
	public Monitor getMonitor(int index) {
		return monitors.get(index);
	}
	public Monitor findMonitor(String name) {
		for (int i=0; i<monitors.size(); i++) {
			Monitor monitor = monitors.get(i);
			if (name!=null && name.equals(monitor.getName()) || name==monitor.getName()) {
				return monitor;
			}
		}
		return null;
	}

	public List<Monitor> getMonitors() {
		return monitors;
	}

	private Map<String, Event> events = new HashMap<>();
	private void register(EventThrowing eventThrowing, String eventCode) {
		Adapter adapter = eventThrowing.getAdapter();
		if(adapter == null || StringUtils.isEmpty(adapter.getName())) {
			throw new IllegalStateException("adapter ["+adapter+"] has no (usable) name");
		}

		//Update the list with potential events that can be thrown
		Event event = events.getOrDefault(eventCode, new Event());
		event.addThrower(eventThrowing);
		events.put(eventCode, event);
	}

	public Map<String, Event> getEvents() {
		return events;
	}

	public static class Event {
		private List<EventThrowing> throwers = new ArrayList<>();

		public Event() {}

		public Event(EventThrowing thrower) {
			throwers.add(thrower);
		}

		public void addThrower(EventThrowing thrower) {
			throwers.add(thrower);
		}

		private List<EventThrowing> getThrowers() {
			return Collections.unmodifiableList(throwers);
		}

		public List<String> getAdapters() {
			List<String> adapters = new ArrayList<>();
			for(EventThrowing eventThrower : getThrowers()) {
				Adapter adapter = eventThrower.getAdapter();
				if(adapter != null && !adapters.contains(adapter.getName())) {
					adapters.add(adapter.getName());
				}
			}
			return adapters;
		}

		public Map<String, List<String>> getSources() {
			Map<String, List<String>> sources = new HashMap<>();
			for(EventThrowing eventThrower : getThrowers()) {
				Adapter adapter = eventThrower.getAdapter(); //TODO null check?
				List<String> sourceNames = sources.getOrDefault(adapter.getName(), new ArrayList<>());
				sourceNames.add(eventThrower.getEventSourceName());
				sources.put(adapter.getName(), sourceNames);
			}
			return sources;
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

	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void start() {
		// Nothing to start?
	}

	@Override
	public void stop() {
		// Nothing to stop?
	}
}
