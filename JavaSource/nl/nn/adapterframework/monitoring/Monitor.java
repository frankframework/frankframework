/*
 * $Log: Monitor.java,v $
 * Revision 1.4  2008-08-07 11:31:27  europe\L190409
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

/**
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class Monitor {
	protected Logger log = LogUtil.getLogger(this);

	private String name;
	private EventTypeEnum type=EventTypeEnum.TECHNICAL;
	private boolean raised=false;
	
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
		for (Iterator it=triggers.iterator(); it.hasNext();) {
			Trigger trigger = (Trigger)it.next();
			trigger.configure();
		}
	}
	
	public void registerEventNotificationListener(Trigger trigger, String eventCode, String thrower) throws MonitorException {
		getOwner().registerEventNotificationListener(trigger,eventCode,thrower);
	}
	
	public void changeState(boolean alarm, SeverityEnum severity, EventThrowing source, String details, Throwable t) throws MonitorException {
		if (destinationSet.size()>0) {
			boolean up=alarm && (!raised || getAlarmSeverityEnum()==null || getAlarmSeverityEnum().compareTo(severity)<0);
			boolean clear=raised && (!alarm || up && getAlarmSeverityEnum()!=null && getAlarmSeverityEnum()!=severity);
			if (clear) {
				if (log.isDebugEnabled()) log.debug(getLogPrefix()+"state ["+getAlarmSeverityEnum()+"] will be cleared");
				SeverityEnum clearSeverity=getAlarmSeverityEnum()!=null?getAlarmSeverityEnum():severity;
				EventThrowing clearSource=getAlarmSource()!=null?getAlarmSource():source;
				changeMonitorState(clearSource, EventTypeEnum.CLEARING, clearSeverity, details, t);
			}
			if (up) {
				if (log.isDebugEnabled()) log.debug(getLogPrefix()+"state ["+getAlarmSeverityEnum()+"] will be raised to ["+severity+"]");
				changeMonitorState(source, getTypeEnum(), severity, details, t);
				setAlarmSource(source);
				setAlarmSeverityEnum(severity);
			}
		}
		raised=alarm;
		notifyReverseTrigger(alarm,source);
	}

	public void changeMonitorState(EventThrowing subSource, EventTypeEnum eventType, SeverityEnum severity, String message, Throwable t) throws MonitorException {
		String eventSource=subSource==null?"unknown":subSource.getEventSourceName();
		if (eventType==null) {
			throw new MonitorException("eventType cannot be null");
		}
		if (severity==null) {
			throw new MonitorException("severity cannot be null");
		}
		
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
//		for (int i=0;i<result.length;i++) {
//			log.debug(getLogPrefix()+"destination["+i+"]=["+result[i]+"]");
//		}
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
	public void setTypeEnum(EventTypeEnum enum) {
		type = enum;
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
	public void setAlarmSeverityEnum(SeverityEnum enum) {
		alarmSeverity = enum;
	}
	public SeverityEnum getAlarmSeverityEnum() {
		return alarmSeverity;
	}

	public EventThrowing getAlarmSource() {
		return alarmSource;
	}
	public String getAlarmSourceName() {
		return alarmSource==null?null:alarmSource.getEventSourceName();
	}
	public void setAlarmSource(EventThrowing source) {
		alarmSource = source;
	}

}
