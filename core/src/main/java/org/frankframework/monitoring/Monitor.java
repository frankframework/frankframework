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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.FrankElement;
import org.frankframework.core.NameAware;
import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;
import org.frankframework.doc.Mandatory;
import org.frankframework.lifecycle.ConfigurableLifecycle;
import org.frankframework.monitoring.ITrigger.TriggerType;
import org.frankframework.monitoring.events.MonitorEvent;
import org.frankframework.util.StringUtil;
import org.frankframework.util.XmlBuilder;

/**
 * <p>Example configuration:</p>
 * <pre>{@code
 * <Monitor name="Receiver Shutdown" destinations="MONITOR_LOG">
 *    <AlarmTrigger severity="WARNING">
 *        <Event>Receiver Shutdown</Event>
 *    </AlarmTrigger>
 *    <ClearingTrigger severity="WARNING">
 *        <Event>Receiver Shutdown</Event>
 *    </ClearingTrigger>
 * </Monitor>
 * }</pre>
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 *
 * @version 2.0
 * @author Niels Meijer
 */
@FrankDocGroup(FrankDocGroupValue.MONITORING)
@Log4j2
public class Monitor implements ConfigurableLifecycle, NameAware, DisposableBean, FrankElement {
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();

	private boolean started = false;
	private boolean isConfigured = false;

	private @Getter String name;
	private @Getter @Setter EventType type = EventType.TECHNICAL;
	private boolean raised = false;
	private @Getter Instant stateChanged = null;

	private int additionalHitCount = 0;
	private @Getter Instant lastHit = null;

	private @Getter @Setter Severity alarmSeverity = null;
	private String eventCode = null;
	private @Getter EventThrowing raisedBy = null;


	private MonitorManager manager = null;

	private final List<ITrigger> triggers = new ArrayList<>();
	private final Set<String> destinations = new HashSet<>();
	private @Getter ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		if (applicationContext instanceof MonitorManager manager) {
			this.manager = manager;
		} else {
			throw new IllegalStateException("ApplicationContext is not a MonitorManager");
		}
	}

	@Override
	public void configure() throws ConfigurationException {
		for(String destination : destinations) { // Monitor should fail is destination does not exist.
			if(getManager().getDestination(destination) == null) {
				throw new ConfigurationException("destination ["+destination+"] does not exist");
			}
		}

		log.debug("monitor [{}] configuring triggers", getName());
		for (ITrigger trigger : triggers) {
			if (!trigger.isConfigured()) {
				trigger.configure();

				// Add the EventListener to the MonitorManager
				manager.addApplicationListener(trigger);
			}
		}

		isConfigured = true;
	}

	public void changeState(TriggerType type, Severity severity, MonitorEvent event) throws MonitorException {
		boolean alarm = type == TriggerType.ALARM;
		boolean up=alarm && (!raised || getAlarmSeverity()==null || getAlarmSeverity().compareTo(severity)<0);
		boolean clear=raised && (!alarm || (up && getAlarmSeverity()!=null && getAlarmSeverity()!=severity));
		if (clear) {
			Severity clearSeverity=getAlarmSeverity()!=null?getAlarmSeverity():severity;
			String originalEventCode = eventCode!=null ? eventCode : event.getEventCode();
			log.info("{}clearing event [{}] state with severity [{}] from source [{}]", getLogPrefix(), originalEventCode, clearSeverity, event.getEventSourceName());

			changeMonitorState(EventType.CLEARING, clearSeverity, originalEventCode, event);
			clearRaisedBy();
		}
		if (up) {
			log.debug("{}state [{}] will be raised to [{}]", this::getLogPrefix, this::getAlarmSeverity, ()->severity);
			changeMonitorState(getType(), severity, event.getEventCode(), event);
			storeRaisedBy(event);
			setAlarmSeverity(severity);
			setLastHit(event.getEventTime());
			setAdditionalHitCount(0);
		} else {
			if (alarm && isHit(severity)) {
				setLastHit(event.getEventTime());
				setAdditionalHitCount(getAdditionalHitCount()+1);
			}
		}
		raised=alarm;
		clearEvents(type);
	}

	private boolean isHit(Severity severity) {
		return getAlarmSeverity()==null || getAlarmSeverity().compareTo(severity)<=0;
	}

	public void changeMonitorState(EventType eventType, Severity severity, String eventCode, MonitorEvent event) throws MonitorException {
		if (eventType==null) {
			throw new MonitorException("eventType cannot be null");
		}
		if (severity==null) {
			throw new MonitorException("severity cannot be null");
		}

		setStateChanged(event.getEventTime());

		for(String destination : destinations) {
			IMonitorDestination monitorAdapter = getManager().getDestination(destination);
			if (log.isDebugEnabled()) log.debug("{}firing event on destination [{}]", getLogPrefix(), destination);

			if (monitorAdapter != null) {
				monitorAdapter.fireEvent(name, eventType, severity, eventCode, event);
			}
		}
	}

	protected void clearEvents(TriggerType alarm) {
		for (ITrigger trigger : triggers) {
			if (trigger.getTriggerType() != alarm) {
				trigger.clearEvents();
			}
		}
	}

	private void storeRaisedBy(MonitorEvent event) {
		eventCode = event.getEventCode();
		raisedBy = event.getSource();
	}

	private void clearRaisedBy() {
		eventCode = null;
		raisedBy = null;
	}

	public XmlBuilder toXml() {
		XmlBuilder monitor=new XmlBuilder("Monitor");
		monitor.addAttribute("name",getName());
		monitor.addAttribute("type",getType().name());
		monitor.addAttribute("destinations",getDestinationsAsString());
		for (ITrigger trigger : triggers) {
			trigger.toXml(monitor);
		}
		return monitor;
	}

	public String getDestinationsAsString() {
		if (destinations.isEmpty()) return null;
		return String.join(",", destinations);
	}

	// Digester setter
	public void setDestinations(String newDestinations) {
		destinations.clear();
		destinations.addAll(StringUtil.split(newDestinations));
	}

	public Set<String> getDestinationSet() {
		return Collections.unmodifiableSet(destinations);
	}
	public void setDestinationSet(Set<String> newDestinations) {
		if (newDestinations==null) {
			if (log.isDebugEnabled()) log.debug("{}clearing destinations", getLogPrefix());
			destinations.clear();
		} else {
			if (log.isDebugEnabled()) log.debug("{}setting destinations to [{}]", getLogPrefix(), newDestinations);
			for(String destination : newDestinations) {
				if(getManager().getDestination(destination) == null) {
					throw new IllegalArgumentException("destination ["+destination+"] does not exist");
				}
			}

			// Only proceed if all destinations exist
			destinations.clear();
			for(String destination : newDestinations) {
				if (log.isDebugEnabled()) log.debug("{}adding destination [{}]", getLogPrefix(), destination);
				destinations.add(destination);
			}
		}
	}

	public void addTrigger(ITrigger trigger) {
		trigger.setMonitor(this);
		triggers.add(trigger);
	}

	public void removeTrigger(ITrigger trigger) {
		int index = triggers.indexOf(trigger);
		if(index > -1) {
			// Remove the EventListener from the MonitorManager
			AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
			factory.destroyBean(trigger);
			triggers.remove(trigger);
		}
	}

	public String getLogPrefix() {
		return "Monitor ["+getName()+"] ";
	}

	private MonitorManager getManager() {
		return manager;
	}

	public List<ITrigger> getTriggers() {
		return triggers;
	}
	public ITrigger getTrigger(int index) {
		return triggers.get(index);
	}

	@Mandatory
	@Override
	public void setName(String string) {
		name = string;
	}

	public void setRaised(boolean b) {
		raised = b;
	}
	public boolean isRaised() {
		return raised;
	}

	private void setStateChanged(Instant date) {
		this.stateChanged = date;
	}

	private void setLastHit(Instant date) {
		lastHit = date;
	}

	public void setAdditionalHitCount(int i) {
		additionalHitCount = i;
	}
	public int getAdditionalHitCount() {
		return additionalHitCount;
	}

	/**
	 * Destroy the monitor and all registered triggers
	 */
	@Override
	public void destroy() {
		log.info("removing monitor [{}]", this);

		// Remove the EventListener from the MonitorManager
		AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
		for (ITrigger trigger : triggers) {
			factory.destroyBean(trigger);
		}
	}

	@Override
	public boolean isAutoStartup() {
		if (!isConfigured) return false; // Don't startup until configured
		return true;
	}

	@Override
	public void start() {
		started = true;
	}

	@Override
	public void stop() {
		started = false;
	}

	@Override
	public boolean isRunning() {
		return started;
	}
}
