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
/*
 * $Log: Monitor.java,v $
 * Revision 1.9  2011-11-30 13:51:43  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.7  2011/06/20 13:21:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Java 5.0 compatibility
 *
 * Revision 1.6  2009/05/13 08:18:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved monitoring: triggers can now be filtered multiselectable on adapterlevel
 *
 * Revision 1.5  2008/08/27 16:16:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added stateChangeDate
 *
 * Revision 1.4  2008/08/07 11:31:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework
 *
 * Revision 1.3  2008/07/24 12:34:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework
 *
 * Revision 1.2  2008/07/17 16:17:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework
 *
 * Revision 1.1  2008/07/14 17:21:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version of flexible monitoring
 *
 */
package nl.nn.adapterframework.monitoring;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

/**
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version $Id$
 */
public class Monitor {
	protected Logger log = LogUtil.getLogger(this);

	private String name;
	private EventTypeEnum type=EventTypeEnum.TECHNICAL;
	private boolean raised=false;
	private Date stateChangeDt=null;
	
	private int additionalHitCount=0;
	private Date lastHit=null;
	
	private SeverityEnum alarmSeverity=null;  
	private EventThrowing alarmSource=null;  

	
	private MonitorManager owner=null;

	private List triggers = new ArrayList();
	private Set destinationSet=new HashSet(); 
	
	public Monitor() {
		super();
	}

	public void register(Object x) {
		MonitorManager.getInstance().addMonitor(this);
	}
	
	public void configure() throws ConfigurationException {
		if (MonitorManager.traceReconfigure && log.isDebugEnabled()) log.debug("monitor ["+getName()+"] configuring triggers");
		for (Iterator it=triggers.iterator(); it.hasNext();) {
			Trigger trigger = (Trigger)it.next();
			trigger.configure();
		}
	}
	
	public void registerEventNotificationListener(Trigger trigger, List eventCodes, Map adapterFilters, boolean filterOnLowerLevelObjects, boolean filterExclusive) throws MonitorException {
		if (MonitorManager.traceReconfigure && log.isDebugEnabled()) log.debug("monitor ["+getName()+"] registerEventNotificationListener for trigger");
		getOwner().registerEventNotificationListener(trigger, eventCodes, adapterFilters, filterOnLowerLevelObjects, filterExclusive);
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
		notifyReverseTrigger(alarm,source);
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
		
		for (Iterator it=destinationSet.iterator();it.hasNext();) {
			String key=(String)it.next();
			IMonitorAdapter monitorAdapter = getOwner().getDestination(key);
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"firing event on destination ["+key+"]");
			if (monitorAdapter!=null) {
				monitorAdapter.fireEvent(eventSource, eventType, severity, getName(), null); 
			}
		}
	}


	protected void notifyReverseTrigger(boolean alarm, EventThrowing source) {
		for (Iterator it=triggers.iterator(); it.hasNext();) {
			Trigger trigger=(Trigger)it.next();
			if (trigger.isAlarm()!=alarm) {
				trigger.notificationOfReverseTrigger(source);
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
		for (Iterator it=triggers.iterator();it.hasNext();) {
			Trigger trigger=(Trigger)it.next();
			trigger.toXml(monitor);
		}
		return monitor;
	}


	public boolean isDestination(String name) {
		return destinationSet.contains(name);
	}
	public String getDestinationsAsString() {
		//log.debug(getLogPrefix()+"calling getDestinationsAsString()");
		String result=null;
		for (Iterator it=getDestinationSet().iterator();it.hasNext();) {
			String item=(String)it.next();
			if (result==null) {
				result=item;
			} else {
				result+=","+item;
			}
		}
		return result;
	}
	public String[] getDestinations() {
		//log.debug(getLogPrefix()+"entering getDestinations()");
		String[] result=new String[destinationSet.size()];
		result=(String[])destinationSet.toArray(result);
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
			Set set=new HashSet();
			for (int i=0;i<newDestinations.length;i++) {
				log.debug(getLogPrefix()+"adding destination ["+newDestinations[i]+"]");
				set.add(newDestinations[i]); 
			}
			setDestinationSet(set);
		}
	}
	public Set getDestinationSet() {
		return destinationSet;
	}
	public void setDestinationSet(Set newDestinations) {
		if (newDestinations==null) {
			log.debug(getLogPrefix()+"clearing destinations");
			destinationSet.clear();
		} else {
			if (log.isDebugEnabled()) {
				String destinations=null;
				for (Iterator it=newDestinations.iterator();it.hasNext();) {
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
		trigger.setOwner(this);
		triggers.add(trigger);
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

	public void setOwner(MonitorManager manager) {
		owner = manager;
	}
	public MonitorManager getOwner() {
		return owner;
	}

	public List getTriggers() {
		return triggers;
	}
	public Trigger getTrigger(int index) {
		return (Trigger)triggers.get(index);
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public void setName(String string) {
		name = string;
	}
	public String getName() {
		return name;
	}

	public void setType(String eventType) {
		setTypeEnum((EventTypeEnum)EventTypeEnum.getEnumMap().get(eventType));
	}
	public String getType() {
		return type==null?null:type.getName();
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
		return alarmSeverity==null?null:alarmSeverity.getName();
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
		getOwner().registerStateChange(date);
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


}
