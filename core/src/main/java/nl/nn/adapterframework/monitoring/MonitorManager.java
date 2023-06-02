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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.doc.FrankDocGroup;
import nl.nn.adapterframework.lifecycle.ConfigurableLifecyleBase;
import nl.nn.adapterframework.monitoring.events.Event;
import nl.nn.adapterframework.monitoring.events.RegisterMonitorEvent;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Manager for Monitoring.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 * 
 * @author Niels Meijer
 * @version 2.0
 */
@FrankDocGroup(name = "Monitoring")
public class MonitorManager extends ConfigurableLifecyleBase implements ApplicationContextAware, ApplicationListener<RegisterMonitorEvent> {

	private @Getter @Setter ApplicationContext applicationContext;
	private List<Monitor> monitors = new ArrayList<>();							// All monitors managed by this MonitorManager
	private Map<String, Event> events = new HashMap<>();						// All events that can be thrown
	private Map<String, IMonitorDestination> destinations = new LinkedHashMap<>();	// All destinations (that can receive status messages) managed by this MonitorManager

	private boolean enabled = AppConstants.getInstance().getBoolean("monitoring.enabled", false);

	/**
	 * (re)configure all destinations and all monitors.
	 * Monitors will register all required eventNotificationListeners.
	 */
	@Override
	public void configure() throws ConfigurationException {
		if (log.isDebugEnabled()) log.debug(getLogPrefix()+"configuring destinations");
		for(String name : destinations.keySet()) {
			IMonitorDestination destination = getDestination(name);
			destination.configure();
		}

		//Only configure Monitors if all destinations were able to configure successfully
		if (log.isDebugEnabled()) log.debug(getLogPrefix()+"configuring monitors");
		for(Monitor monitor : monitors) {
			monitor.configure();
		}
	}

	private String getLogPrefix() {
		return "Manager@"+this.hashCode();
	}

	public void registerDestination(IMonitorDestination monitorAdapter) {
		destinations.put(monitorAdapter.getName(), monitorAdapter);
	}
	public IMonitorDestination getDestination(String name) {
		return destinations.get(name);
	}
	public Map<String, IMonitorDestination> getDestinations() {
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
			log.debug(getLogPrefix()+"registerEvent ["+eventCode+"] for adapter ["+(thrower.getAdapter() == null ? null : thrower.getAdapter().getName())+"] object ["+thrower.getEventSourceName()+"]");
		}

		registerEvent(thrower, eventCode);
	}

	public void addMonitor(Monitor monitor) {
		monitor.setManager(this);
		monitors.add(monitor);
	}

	public void removeMonitor(Monitor monitor) {
		int index = monitors.indexOf(monitor);
		if(index > -1) {
			String name = monitor.getName();
			AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
			factory.destroyBean(monitor);
			monitors.remove(index);
			log.debug("removing monitor [{}] from MonitorManager [{}]", name, this);
		}
	}

	public Monitor getMonitor(int index) {
		return monitors.get(index);
	}
	public Monitor findMonitor(String name) {
		if(name == null) {
			return null;
		}

		for (int i=0; i<monitors.size(); i++) {
			Monitor monitor = monitors.get(i);
			if (name.equals(monitor.getName())) {
				return monitor;
			}
		}
		return null;
	}

	public List<Monitor> getMonitors() {
		//Monitors may not be added nor removed directly
		return Collections.unmodifiableList(monitors);
	}

	private void registerEvent(EventThrowing eventThrowing, String eventCode) {
		Adapter adapter = eventThrowing.getAdapter();
		if(adapter == null || StringUtils.isEmpty(adapter.getName())) {
			throw new IllegalStateException("adapter ["+adapter+"] has no (usable) name");
		}

		//Update the list with potential events that can be thrown
		Event event = events.computeIfAbsent(eventCode, e->new Event());
		event.addThrower(eventThrowing);
		events.put(eventCode, event);
	}

	public Map<String, Event> getEvents() {
		return events;
	}

	public XmlBuilder toXml() {
		XmlBuilder configXml=new XmlBuilder("monitoring");
		configXml.addAttribute("enabled",isEnabled());
		for(String name : destinations.keySet()) {
			IMonitorDestination ma=getDestination(name);

			XmlBuilder destinationXml=new XmlBuilder("destination");
			destinationXml.addAttribute("name",ma.getName());
			destinationXml.addAttribute("className",ma.getClass().getName());

			configXml.addSubElement(ma.toXml());
		}
		for (int i=0; i<monitors.size(); i++) {
			Monitor monitor=getMonitor(i);
			configXml.addSubElement(monitor.toXml());
		}

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
