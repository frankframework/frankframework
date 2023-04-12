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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.core.IConfigurationAware;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.doc.FrankDocGroup;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * @author  Gerrit van Brakel
 * @since   4.9
 * 
 * @version 2.0
 * @author Niels Meijer
 */
@FrankDocGroup(name = "Monitoring")
public class Monitor implements IConfigurationAware, INamedObject, DisposableBean {
	protected Logger log = LogUtil.getLogger(this);
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();

	private @Getter String name;
	private @Getter @Setter EventType type = EventType.TECHNICAL;
	private boolean raised = false;
	private Date stateChangeDt = null;

	private int additionalHitCount = 0;
	private Date lastHit = null;

	private @Getter @Setter Severity alarmSeverity = null;
	private EventThrowing alarmSource = null;


	private MonitorManager manager = null;

	private List<ITrigger> triggers = new ArrayList<>();
	private Set<String> destinations = new HashSet<>();
	private @Getter @Setter ApplicationContext applicationContext;

	public void configure() {
		for(String destination : destinations) {
			if(getManager().getDestination(destination) == null) {
				throw new IllegalArgumentException("destination ["+destination+"] does not exist");
			}
		}

		if (log.isDebugEnabled()) log.debug("monitor ["+getName()+"] configuring triggers");
		for (Iterator<ITrigger> it=triggers.iterator(); it.hasNext();) {
			ITrigger trigger = it.next();
			if(!trigger.isConfigured()) {
				trigger.configure();
				((ConfigurableApplicationContext)applicationContext).addApplicationListener(trigger);
			}
		}
	}

	public void changeState(Date date, boolean alarm, Severity severity, EventThrowing source, String details, Throwable t) throws MonitorException {
		boolean up=alarm && (!raised || getAlarmSeverity()==null || getAlarmSeverity().compareTo(severity)<0);
		boolean clear=raised && (!alarm || up && getAlarmSeverity()!=null && getAlarmSeverity()!=severity);
		if (clear) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"state ["+getAlarmSeverity()+"] will be cleared");
			Severity clearSeverity=getAlarmSeverity()!=null?getAlarmSeverity():severity;
			EventThrowing clearSource=getAlarmSource()!=null?getAlarmSource():source;
			changeMonitorState(date, clearSource, EventType.CLEARING, clearSeverity, details, t);
		}
		if (up) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"state ["+getAlarmSeverity()+"] will be raised to ["+severity+"]");
			changeMonitorState(date, source, getType(), severity, details, t);
			setAlarmSource(source);
			setAlarmSeverityEnum(severity);
			setLastHit(date);
			setAdditionalHitCount(0);
		} else {
			if (alarm && isHit(severity)) {
				setLastHit(date);
				setAdditionalHitCount(getAdditionalHitCount()+1);
			}
		}
		raised=alarm;
		clearEvents(alarm);
	}

	private boolean isHit(Severity severity) {
		return (getAlarmSeverity()==null || getAlarmSeverity().compareTo(severity)<=0);
	}

	public void changeMonitorState(Date date, EventThrowing subSource, EventType eventType, Severity severity, String message, Throwable t) throws MonitorException {
		String eventSource=subSource==null?"":subSource.getEventSourceName();
		if (eventType==null) {
			throw new MonitorException("eventType cannot be null");
		}
		if (severity==null) {
			throw new MonitorException("severity cannot be null");
		}
		setStateChangeDt(date);

		for(String destination : destinations) {
			IMonitorAdapter monitorAdapter = getManager().getDestination(destination);
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"firing event on destination ["+destination+"]");

			if (monitorAdapter != null) {
				monitorAdapter.fireEvent(eventSource, eventType, severity, getName(), null);
			}
		}
	}

	protected void clearEvents(boolean alarm) {
		for (Iterator<ITrigger> it=triggers.iterator(); it.hasNext();) {
			ITrigger trigger = it.next();
			if (trigger.isAlarm()!=alarm) {
				trigger.clearEvents();
			}
		}
	}

	public XmlBuilder getStatusXml() {
		XmlBuilder monitor=new XmlBuilder("monitor");
		monitor.addAttribute("name",getName());
		monitor.addAttribute("raised",isRaised());
		if (stateChangeDt!=null) {
			monitor.addAttribute("changed",getStateChangeDtStr());
		}
		if (isRaised()) {
			monitor.addAttribute("severity", getAlarmSeverity().name());
			EventThrowing source = getAlarmSource();
			if (source!=null) {
				monitor.addAttribute("source",source.getEventSourceName());
			}
		}
		return monitor;
	}
	public XmlBuilder toXml() {
		XmlBuilder monitor=new XmlBuilder("monitor");
		monitor.addAttribute("name",getName());
		monitor.addAttribute("type",getType().name());
		monitor.addAttribute("destinations",getDestinationsAsString());
		for (Iterator<ITrigger> it=triggers.iterator();it.hasNext();) {
			ITrigger trigger=it.next();
			trigger.toXml(monitor);
		}
		return monitor;
	}

	public String getDestinationsAsString() {
		String result=null;
		for(String destination : getDestinationSet()) {
			if (result==null) {
				result=destination;
			} else {
				result+=","+destination;
			}
		}
		return result;
	}

	//Digester setter
	public void setDestinations(String newDestinations) {
		destinations.clear();
		StringTokenizer st=new StringTokenizer(newDestinations,",");
		while (st.hasMoreTokens()) {
			destinations.add(st.nextToken());
		}
	}

	public Set<String> getDestinationSet() {
		return Collections.unmodifiableSet(destinations);
	}
	public void setDestinationSet(Set<String> newDestinations) {
		if (newDestinations==null) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"clearing destinations");
			destinations.clear();
		} else {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"setting destinations to ["+newDestinations+"]");
			for(String destination : newDestinations) {
				if(getManager().getDestination(destination) == null) {
					throw new IllegalArgumentException("destination ["+destination+"] does not exist");
				}
			}

			//Only proceed if all destinations exist
			destinations.clear();
			for(String destination : newDestinations) {
				if (log.isDebugEnabled()) log.debug(getLogPrefix()+"adding destination ["+destination+"]");
				destinations.add(destination);
			}
		}
	}

	public void registerTrigger(ITrigger trigger) {
		trigger.setMonitor(this);
		triggers.add(trigger);
	}

	public void removeTrigger(ITrigger trigger) {
		int index = triggers.indexOf(trigger);
		if(index > -1) {
			AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
			factory.destroyBean(trigger);
			triggers.remove(trigger);
		}
	}

	public String getLogPrefix() {
		return "Monitor ["+getName()+"] ";
	}

	public void setManager(MonitorManager manager) {
		this.manager = manager;
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

	public void setAlarmSeverityEnum(Severity enumeration) {
		alarmSeverity = enumeration;
	}

	public EventThrowing getAlarmSource() {
		return alarmSource;
	}
	public void setAlarmSource(EventThrowing source) {
		alarmSource = source;
	}

	public void setStateChangeDt(Date date) {
		stateChangeDt = date;
		getManager().registerStateChange(date);
	}
	public Date getStateChangeDt() {
		return stateChangeDt;
	}
	public String getStateChangeDtStr() {
		if (stateChangeDt!=null) {
			return DateUtils.format(stateChangeDt,DateUtils.FORMAT_FULL_GENERIC);
		}
		return "";
	}

	public void setLastHit(Date date) {
		lastHit = date;
	}
	public Date getLastHit() {
		return lastHit;
	}
	public String getLastHitStr() {
		if (lastHit!=null) {
			return DateUtils.format(lastHit,DateUtils.FORMAT_FULL_GENERIC);
		}
		return "";
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
		log.info("removing monitor ["+this+"]");

		AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
		for (ITrigger trigger : triggers) {
			factory.destroyBean(trigger);
		}
	}

}
