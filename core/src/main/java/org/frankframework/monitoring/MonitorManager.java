/*
   Copyright 2013 Nationale-Nederlanden, 2021-2025 WeAreFrank!

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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.support.GenericApplicationContext;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;
import org.frankframework.lifecycle.ConfigurableLifecycle;
import org.frankframework.lifecycle.ConfiguringLifecycleProcessor;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.monitoring.events.Event;
import org.frankframework.monitoring.events.RegisterMonitorEvent;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.XmlBuilder;

/**
 * Manager for Monitoring.
 * <p>
 * Configure/start/stop lifecycles are managed by Spring.
 *
 * @author Niels Meijer
 * @version 2.1
 */
@Log4j2
@FrankDocGroup(FrankDocGroupValue.MONITORING)
public class MonitorManager extends GenericApplicationContext implements ConfigurableLifecycle, ApplicationContextAware, ApplicationListener<RegisterMonitorEvent>, InitializingBean {

	private final Map<String, Event> events = new ConcurrentHashMap<>();                 // All events that can be thrown

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		setParent(applicationContext);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (isActive()) {
			throw new LifecycleException("unable to refresh, MonitorManager is already active");
		}

		refresh();
	}

	@Override
	protected void initLifecycleProcessor() {
		ConfiguringLifecycleProcessor defaultProcessor = new ConfiguringLifecycleProcessor();
		defaultProcessor.setBeanFactory(getBeanFactory());
		getBeanFactory().registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, defaultProcessor);
		super.initLifecycleProcessor();
	}

	/**
	 * (re)configure all destinations and all monitors.
	 * Monitors will register all required eventNotificationListeners.
	 */
	@Override
	public void configure() throws ConfigurationException {
		if (!isActive()) {
			throw new LifecycleException("MonitorManager is not active");
		}
		log.debug("configuring MonitorManager [{}]", this::getName);

		// Trigger a configure on all (Configurable) Lifecycle beans
		LifecycleProcessor lifecycle = getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
		if (!(lifecycle instanceof ConfigurableLifecycle configurableLifecycle)) {
			throw new ConfigurationException("wrong lifecycle processor found, unable to configure beans");
		}
		configurableLifecycle.configure();
	}

	@Override
	public int getPhase() {
		return 300;
	}

	private String getName() {
		return "Manager@"+this.hashCode();
	}

	public void addDestination(IMonitorDestination monitorDestination) {
		log.debug("registering destination [{}] with MonitorManager [{}]", monitorDestination::toString, this::getName);
		if(monitorDestination.getName() == null) {
			throw new IllegalStateException("destination has no name");
		}

		SpringUtils.registerSingleton(this, monitorDestination.getName(), monitorDestination);
		log.debug("Configuration [{}] registered adapter [{}]", this::getName, monitorDestination::toString);
	}

	@Override
	public void onApplicationEvent(RegisterMonitorEvent event) {
		EventThrowing thrower = event.getSource();
		String eventCode = event.getEventCode();

		if (log.isDebugEnabled()) {
			log.debug("{} registerEvent [{}] for adapter [{}] object [{}]", getName(), eventCode, thrower.getAdapter() == null ? null : thrower.getAdapter()
					.getName(), thrower.getEventSourceName());
		}

		registerEvent(thrower, eventCode);
	}

	public void addMonitor(Monitor monitor) {
		log.debug("registering monitor [{}] with MonitorManager [{}]", monitor::toString, this::getName);
		if(monitor.getName() == null) {
			throw new IllegalStateException("destination has no name");
		}

		SpringUtils.registerSingleton(this, monitor.getName(), monitor);
		log.debug("Configuration [{}] registered adapter [{}]", this::getName, monitor::toString);
	}

	public void removeMonitor(Monitor monitor) {
		DefaultListableBeanFactory cbf = (DefaultListableBeanFactory) getAutowireCapableBeanFactory();
		String name = monitor.getName();
		getMonitors()
				.keySet()
				.stream()
				.filter(name::equals)
				.forEach(cbf::destroySingleton);
		log.debug("removing monitor [{}] from MonitorManager [{}]", monitor::getName, this::getName);
	}

//	public Monitor getMonitor(int index) {
//		return getMonitors().get(index);
//	}

	public Optional<Monitor> findMonitor(String name) {
		if (name == null) {
			return Optional.empty();
		}

		return Optional.of(getMonitors().get(name));
	}

	private void registerEvent(EventThrowing eventThrowing, String eventCode) {
		Adapter adapter = eventThrowing.getAdapter();
		if(adapter == null || StringUtils.isEmpty(adapter.getName())) {
			throw new IllegalStateException("adapter ["+adapter+"] has no (usable) name");
		}

		// Update the list with potential events that can be thrown
		Event event = events.computeIfAbsent(eventCode, e->new Event());
		event.addThrower(eventThrowing);
		events.put(eventCode, event);
	}

	public Map<String, Event> getEvents() {
		return events;
	}

	public XmlBuilder toXml() {
		XmlBuilder configXml = new XmlBuilder("monitoring");
		for (String name : getDestinations().keySet()) {
			IMonitorDestination ma = getDestination(name);

			XmlBuilder destinationXml = new XmlBuilder("destination");
			destinationXml.addAttribute("name", ma.getName());
			destinationXml.addAttribute("className", ma.getClass().getName());

			configXml.addSubElement(ma.toXml());
		}

		for (String name : getMonitors().keySet()) {
			Monitor monitor = getMonitor(name);
			configXml.addSubElement(monitor.toXml());
		}

		return configXml;
	}

	@Nullable
	public Monitor getMonitor(String name) {
		return getMonitors().get(name);
	}
	@Nonnull
	// Monitors may not be added nor removed directly
	public final Map<String, Monitor> getMonitors() {
		Map<String, Monitor> adapters = getBeansOfType(Monitor.class);
		return Collections.unmodifiableMap(adapters);
	}

	@Nullable
	public IMonitorDestination getDestination(String name) {
		return getDestinations().get(name);
	}
	@Nonnull
	public final Map<String, IMonitorDestination> getDestinations() {
		Map<String, IMonitorDestination> adapters = getBeansOfType(IMonitorDestination.class);
		return Collections.unmodifiableMap(adapters);
	}

}
