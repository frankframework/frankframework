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

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

import lombok.Setter;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * @author  Gerrit van Brakel
 * @since   4.9
 * 
 * @version 2.0
 * @author Niels Meijer
 */
public class Monitor implements ApplicationContextAware, DisposableBean {
	protected Logger log = LogUtil.getLogger(this);

	private String name;
	private EventTypeEnum type = EventTypeEnum.TECHNICAL;
	private boolean raised = false;
	private Date stateChangeDt = null;

	private int additionalHitCount=0;
	private Date lastHit=null;

	private SeverityEnum alarmSeverity=null;
	private EventThrowing alarmSource=null;


	private MonitorManager manager = null;

	private List<Trigger> triggers = new ArrayList<Trigger>();
	private Set<String> destinationSet = new HashSet<String>(); 
	private @Setter ApplicationContext applicationContext;

	public Monitor() {
		super();
	}

	public void register(MonitorManager mm) {
		mm.addMonitor(this);
	}

	public void configure() {
		if (log.isDebugEnabled()) log.debug("monitor ["+getName()+"] configuring triggers");

		for (Iterator<Trigger> it=triggers.iterator(); it.hasNext();) {
			Trigger trigger = it.next();
			if(!trigger.isConfigured()) {
				trigger.configure();
				((ConfigurableApplicationContext)applicationContext).addApplicationListener(trigger);
			}
		}
	}

	public void changeState(Date date, boolean alarm, SeverityEnum severity, EventThrowing source, String details, Throwable t) throws MonitorException {
		boolean hit=alarm && (getAlarmSeverityEnum()==null || getAlarmSeverityEnum().compareTo(severity)<=0);
		boolean up=alarm && (!raised || getAlarmSeverityEnum()==null || getAlarmSeverityEnum().compareTo(severity)<0);
		boolean clear=raised && (!alarm || up && getAlarmSeverityEnum()!=null && getAlarmSeverityEnum()!=severity);
		if (clear) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"state ["+getAlarmSeverityEnum()+"] will be cleared");
			SeverityEnum clearSeverity=getAlarmSeverityEnum()!=null?getAlarmSeverityEnum():severity;
			EventThrowing clearSource=getAlarmSource()!=null?getAlarmSource():source;
			changeMonitorState(date, clearSource, EventTypeEnum.CLEARING, clearSeverity, details, t);
		}
		if (up) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"state ["+getAlarmSeverityEnum()+"] will be raised to ["+severity+"]");
			changeMonitorState(date, source, getTypeEnum(), severity, details, t);
			setAlarmSource(source);
			setAlarmSeverityEnum(severity);
			setLastHit(date);
			setAdditionalHitCount(0);
		} else {
			if (hit) {
				setLastHit(date);
				setAdditionalHitCount(getAdditionalHitCount()+1);
			}
		}
		raised=alarm;
		clearEvents(alarm);
	}

	public void changeMonitorState(Date date, EventThrowing subSource, EventTypeEnum eventType, SeverityEnum severity, String message, Throwable t) throws MonitorException {
		String eventSource=subSource==null?"":subSource.getEventSourceName();
		if (eventType==null) {
			throw new MonitorException("eventType cannot be null");
		}
		if (severity==null) {
			throw new MonitorException("severity cannot be null");
		}
		setStateChangeDt(date);

		for(String destination : destinationSet) {
			IMonitorAdapter monitorAdapter = getManager().getDestination(destination);
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"firing event on destination ["+destination+"]");

			if (monitorAdapter!=null) {
				monitorAdapter.fireEvent(eventSource, eventType, severity, getName(), null); 
			}
		}
	}

	protected void clearEvents(boolean alarm) {
		for (Iterator<Trigger> it=triggers.iterator(); it.hasNext();) {
			Trigger trigger=(Trigger)it.next();
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
			monitor.addAttribute("severity",getAlarmSeverity());
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
		monitor.addAttribute("type",getType());
		monitor.addAttribute("destinations",getDestinationsAsString());
		for (Iterator<Trigger> it=triggers.iterator();it.hasNext();) {
			Trigger trigger=(Trigger)it.next();
			trigger.toXml(monitor);
		}
		return monitor;
	}

	public String getDestinationsAsString() {
		//log.debug(getLogPrefix()+"calling getDestinationsAsString()");
		String result=null;
		for (Iterator<String> it=getDestinationSet().iterator();it.hasNext();) {
			String item=(String)it.next();
			if (result==null) {
				result=item;
			} else {
				result+=","+item;
			}
		}
		return result;
	}

	public void setDestinations(String newDestinations) {
//		log.debug(getLogPrefix()+"entering setDestinations(String)");
		destinationSet.clear();
		StringTokenizer st=new StringTokenizer(newDestinations,",");
		while (st.hasMoreTokens()) {
			String token=st.nextToken();
//			log.debug(getLogPrefix()+"adding destination ["+token+"]");
			destinationSet.add(token);
		}
	}
	public void setDestinations(String[] newDestinations) {
		if (newDestinations.length==1) {
			log.debug("assuming single string, separated by commas");
			destinationSet.clear();
			StringTokenizer st=new StringTokenizer(newDestinations[0],",");
			while (st.hasMoreTokens()) {
				String token=st.nextToken();
				log.debug(getLogPrefix()+"adding destination ["+token+"]");
				destinationSet.add(token);			
			}
		} else {
			log.debug(getLogPrefix()+"entering setDestinations(String[])");
			Set<String> set=new HashSet<String>();
			for (int i=0;i<newDestinations.length;i++) {
				log.debug(getLogPrefix()+"adding destination ["+newDestinations[i]+"]");
				set.add(newDestinations[i]); 
			}
			setDestinationSet(set);
		}
	}

	public Set<String> getDestinationSet() {
		return Collections.unmodifiableSet(destinationSet);
	}
	public void setDestinationSet(Set<String> newDestinations) {
		if (newDestinations==null) {
			log.debug(getLogPrefix()+"clearing destinations");
			destinationSet.clear();
		} else {
			if (log.isDebugEnabled()) {
				String destinations=null;
				for (Iterator<String> it=newDestinations.iterator();it.hasNext();) {
					if (destinations!=null) {
						destinations+=","+(String)it.next();
					} else {
						destinations=(String)it.next();
					}
				}
				log.debug(getLogPrefix()+"setting destinations to ["+destinations+"]");
			}
//			log.debug("size before retain all ["+destinationSet.size()+"]" );
			destinationSet.retainAll(newDestinations);
//			log.debug("size after retain all ["+destinationSet.size()+"]" );
			destinationSet.addAll(newDestinations);
//			log.debug("size after add all ["+destinationSet.size()+"]" );
		}
	}

	public void registerTrigger(Trigger trigger) {
		trigger.setMonitor(this);
		triggers.add(trigger);
	}

	public void removeTrigger(Trigger trigger) {
		int index = triggers.indexOf(trigger);
		if(index > -1) {
			AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
			factory.destroyBean(trigger);
			triggers.remove(trigger);
		}
	}

	public void registerAlarm(Trigger trigger) {
		trigger.setAlarm(true);
		registerTrigger(trigger);
	}
	public void registerClearing(Trigger trigger) {
		trigger.setAlarm(false);
		registerTrigger(trigger);
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

	public List<Trigger> getTriggers() {
		return triggers;
	}
	public Trigger getTrigger(int index) {
		return triggers.get(index);
	}

	public void setName(String string) {
		name = string;
	}
	public String getName() {
		return name;
	}

	public void setType(String eventType) {
		setTypeEnum(Misc.parse(EventTypeEnum.class, eventType));
	}
	public String getType() {
		return type==null?null:type.name();
	}
	public void setTypeEnum(EventTypeEnum enumeration) {
		type = enumeration;
	}
	public EventTypeEnum getTypeEnum() {
		return type;
	}

	public void setRaised(boolean b) {
		raised = b;
	}
	public boolean isRaised() {
		return raised;
	}

	public String getAlarmSeverity() {
		return alarmSeverity==null?null:alarmSeverity.name();
	}
	public void setAlarmSeverityEnum(SeverityEnum enumeration) {
		alarmSeverity = enumeration;
	}
	public SeverityEnum getAlarmSeverityEnum() {
		return alarmSeverity;
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
		for (Trigger trigger : triggers) {
			factory.destroyBean(trigger);
		}
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toStringExclude(this, "applicationContext");
	}
}
