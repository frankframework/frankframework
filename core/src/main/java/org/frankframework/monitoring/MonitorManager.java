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

import java.util.Arrays;
import java.util.Collections;
import java.util.EventListener;
import java.util.List;
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
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ResolvableType;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;
import org.frankframework.lifecycle.ConfigurableLifecycle;
import org.frankframework.lifecycle.ConfiguringLifecycleProcessor;
import org.frankframework.lifecycle.LazyLoadingEventListener;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.monitoring.events.Event;
import org.frankframework.monitoring.events.MonitorEvent;
import org.frankframework.monitoring.events.RegisterMonitorEvent;
import org.frankframework.util.RunState;
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
public class MonitorManager extends GenericApplicationContext implements ConfigurableLifecycle, ApplicationContextAware, ApplicationListener<MonitorEvent>, InitializingBean {

	private @Getter RunState state = RunState.STOPPED;
	private @Getter boolean configured = false;
	private final Map<String, Event> events = new ConcurrentHashMap<>(); // All events that can be thrown
	private ApplicationEventMulticaster applicationEventMulticaster;

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

		applicationEventMulticaster = getBeanFactory().getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
	}

	@Override
	protected void initLifecycleProcessor() {
		ConfiguringLifecycleProcessor defaultProcessor = new ConfiguringLifecycleProcessor();
		defaultProcessor.setBeanFactory(getBeanFactory());
		getBeanFactory().registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, defaultProcessor);
		super.initLifecycleProcessor();
	}

	@Override
	public void start() {
		log.info("starting MonitorManager [{}]", this::getName);
		if (!configured) {
			throw new IllegalStateException("cannot start monitors that are not configured");
		}

		super.start();
		state = RunState.STARTED;
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
		state = RunState.STARTING;
		log.debug("configuring MonitorManager [{}]", this::getName);

		// Trigger a configure on all (Configurable) Lifecycle beans
		LifecycleProcessor lifecycle = getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
		if (!(lifecycle instanceof ConfigurableLifecycle configurableLifecycle)) {
			throw new ConfigurationException("wrong lifecycle processor found, unable to configure beans");
		}
		configurableLifecycle.configure();
		configured = true;
	}

	@Override
	public void stop() {
		log.info("stopping MonitorManager [{}]", this::getName);
		state = RunState.STOPPING;
		try {
			super.stop();
		} finally {
			state = RunState.STOPPED;
		}
	}

	@Override
	public int getPhase() {
		return 300;
	}

	@Override
	public boolean isRunning() {
		return getState() == RunState.STARTED && super.isRunning();
	}

	private String getName() {
		return getDisplayName();
	}

	// We do not want all listeners to be initialized upon context startup. Hence listeners implementing LazyLoadingEventListener will be excluded from the beanType[].
	@Override
	public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		if(type.isAssignableFrom(ApplicationListener.class)) {
			List<String> blacklist = Arrays.asList(super.getBeanNamesForType(LazyLoadingEventListener.class, includeNonSingletons, allowEagerInit));
			List<String> beanNames = Arrays.asList(super.getBeanNamesForType(type, includeNonSingletons, allowEagerInit));
			log.info("removing LazyLoadingEventListeners {} from Spring auto-magic event-based initialization", blacklist);

			return beanNames.stream().filter(str -> !blacklist.contains(str)).toArray(String[]::new);
		}
		return super.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}

	/**
	 * Propagate the events to their respected {@link EventListener}.
	 * Does not send events to the parent context (if any).
	 */
	@Override
	public void onApplicationEvent(MonitorEvent event) {
		if (event instanceof RegisterMonitorEvent monitorEvent) {
			registerEvent(monitorEvent);
		} else {
			ResolvableType type = ResolvableType.forInstance(event);
			applicationEventMulticaster.multicastEvent(event, type);
		}
	}

	private void registerEvent(RegisterMonitorEvent registerEvent) {
		EventThrowing eventThrowing = registerEvent.getSource();
		String eventCode = registerEvent.getEventCode();
		log.debug("{} registerEvent [{}] for adapter [{}] object [{}]", this::getName, () -> eventCode, eventThrowing::getAdapter, eventThrowing::getEventSourceName);

		Adapter adapter = eventThrowing.getAdapter();
		if(adapter == null || StringUtils.isEmpty(adapter.getName())) {
			throw new IllegalStateException("adapter ["+adapter+"] has no (usable) name");
		}

		// Update the list with potential events that can be thrown
		Event event = events.computeIfAbsent(eventCode, e -> new Event());
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

	// Remove + Add monitor

	public void addMonitor(Monitor monitor) {
		log.debug("registering monitor [{}] with MonitorManager [{}]", monitor::getName, this::getName);
		if(monitor.getName() == null) {
			throw new IllegalStateException("destination has no name");
		}

		SpringUtils.registerSingleton(this, monitor.getName(), monitor);
		log.info("MonitorManager [{}] registered monitor [{}]", this::getName, monitor::getName);
	}

	// Method for runtime Monitor updates
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

	public Optional<Monitor> findMonitor(String name) {
		if (name == null) {
			return Optional.empty();
		}

		return Optional.of(getMonitors().get(name));
	}

	@Nullable
	public Monitor getMonitor(String name) {
		return getMonitors().get(name);
	}

	// Monitors may not be added nor removed directly
	@Nonnull
	public final Map<String, Monitor> getMonitors() {
		Map<String, Monitor> adapters = getBeansOfType(Monitor.class);
		return Collections.unmodifiableMap(adapters);
	}

	// Add Monitor Destination

	public void addDestination(IMonitorDestination monitorDestination) {
		log.debug("registering monitor destination [{}] with MonitorManager [{}]", monitorDestination::toString, this::getName);
		if(monitorDestination.getName() == null) {
			throw new IllegalStateException("destination has no name");
		}

		SpringUtils.registerSingleton(this, monitorDestination.getName(), monitorDestination);
		log.info("MonitorManager [{}] registered monitor destination [{}]", this::getName, monitorDestination::getName);
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
