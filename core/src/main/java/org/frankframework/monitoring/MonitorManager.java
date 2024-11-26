/*
   Copyright 2013 Nationale-Nederlanden, 2021-2024 WeAreFrank!

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

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;
import org.frankframework.lifecycle.AbstractConfigurableLifecyle;
import org.frankframework.monitoring.events.Event;
import org.frankframework.monitoring.events.RegisterMonitorEvent;
import org.frankframework.util.XmlBuilder;

/**
 * Manager for Monitoring.
 * <p>
 * Configure/start/stop lifecycles are managed by Spring.
 *
 * @author Niels Meijer
 * @version 2.0
 */
@FrankDocGroup(FrankDocGroupValue.MONITORING)
public class MonitorManager extends AbstractConfigurableLifecyle implements ApplicationContextAware, ApplicationListener<RegisterMonitorEvent> {

	private @Getter @Setter ApplicationContext applicationContext;
	private final List<Monitor> monitors = new ArrayList<>();                            // All monitors managed by this MonitorManager
	private final Map<String, Event> events = Collections.synchronizedMap(new HashMap<>());                           // All events that can be thrown
	private final Map<String, IMonitorDestination> destinations = new LinkedHashMap<>(); // All destinations (that can receive status messages) managed by this MonitorManager

	/**
	 * (re)configure all destinations and all monitors.
	 * Monitors will register all required eventNotificationListeners.
	 */
	@Override
	public void configure() throws ConfigurationException {
		if (log.isDebugEnabled()) log.debug("{}configuring destinations", getLogPrefix());
		for(String name : destinations.keySet()) {
			IMonitorDestination destination = getDestination(name);
			destination.configure();
		}

		//Only configure Monitors if all destinations were able to configure successfully
		if (log.isDebugEnabled()) log.debug("{}configuring monitors", getLogPrefix());
		for(Monitor monitor : monitors) {
			monitor.configure();
		}
	}

	@Override
	public int getPhase() {
		return 300;
	}

	private String getLogPrefix() {
		return "Manager@"+this.hashCode();
	}

	public void addDestination(IMonitorDestination monitorAdapter) {
		destinations.put(monitorAdapter.getName(), monitorAdapter);
	}
	public IMonitorDestination getDestination(String name) {
		return destinations.get(name);
	}
	public Map<String, IMonitorDestination> getDestinations() {
		return destinations;
	}

	@Override
	public void onApplicationEvent(RegisterMonitorEvent event) {
		EventThrowing thrower = event.getSource();
		String eventCode = event.getEventCode();

		if (log.isDebugEnabled()) {
			log.debug("{} registerEvent [{}] for adapter [{}] object [{}]", getLogPrefix(), eventCode, thrower.getAdapter() == null ? null : thrower.getAdapter()
					.getName(), thrower.getEventSourceName());
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

	@Override
	public void start() {
		// Nothing to start?
	}

	@Override
	public void stop() {
		// Nothing to stop?
	}
}
