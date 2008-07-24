/*
 * $Log: Monitor.java,v $
 * Revision 1.3  2008-07-24 12:34:00  europe\L190409
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
	private String guardedObject;
	private boolean raised=false;
	private SeverityEnum severity=null;  

	
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
	
	public void registerEventNotificationListener(Trigger trigger, String eventCode) throws MonitorException {
		getOwner().registerEventNotificationListener(trigger,eventCode,getGuardedObject());
	}
	
	public void changeState(boolean alarm, SeverityEnum severity, EventThrowing source, String details, Throwable t) throws MonitorException {
		if (destinationSet.size()>0) {
			boolean up=alarm && (!raised || getSeverityEnum()==null || getSeverityEnum().compareTo(severity)<=0);
			boolean clear=raised && (!alarm || up && getSeverityEnum()!=null && getSeverityEnum()!=severity);
			if (clear) {
				SeverityEnum clearSeverity=getSeverityEnum()!=null?getSeverityEnum():severity;
				changeMonitorState(source, EventTypeEnum.CLEARING, clearSeverity, details, t);
			}
			if (up) {
				changeMonitorState(source, getTypeEnum(), severity, details, t);
			}
		}
		raised=alarm;
		setSeverityEnum(severity);
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
			if (monitorAdapter!=null) {
				monitorAdapter.fireEvent(eventSource, eventType, severity, message, t); 
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

	public XmlBuilder toXml(int index, boolean configOnly) {
		XmlBuilder monitor=new XmlBuilder("monitor");
		if (index>=0 && !configOnly) {
			monitor.addAttribute("index",index);
		}
		monitor.addAttribute("name",getName());
		monitor.addAttribute("type",getType());
		monitor.addAttribute("guardedObject",getGuardedObject());
		monitor.addAttribute("destinations",getDestinationsAsString());
		if (!configOnly) {
			monitor.addAttribute("raised",isRaised());
		}
		if (getSeverity()!=null) {
			monitor.addAttribute("severity",getSeverity());
		}
		for (Iterator it=triggers.iterator();it.hasNext();) {
			Trigger trigger=(Trigger)it.next();
			trigger.toXml(monitor);
		}
		return monitor;
	}

	public Set getAllDestinations() {
		return getOwner().getDestinations().keySet();
	}

	public void setDestinationX(String name, boolean value) {
		if (value) {
			destinationSet.add(name);
		} else {
			destinationSet.remove(name);
		}
	}
	public boolean isDestination(String name) {
		return destinationSet.contains(name);
	}
	public String getDestinationsAsString() {
		log.debug(getLogPrefix()+"calling getDestinationsAsString()");
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
		log.debug(getLogPrefix()+"calling getDestinations()");
		String[] result=new String[destinationSet.size()];
		result=(String[])destinationSet.toArray(result);
		for (int i=0;i<result.length;i++) {
			log.debug(getLogPrefix()+"destination["+i+"]=["+result[i]+"]");
		}
		return result;
	}
	public void setDestinations(String newDestinations) {
		destinationSet.clear();
		StringTokenizer st=new StringTokenizer(newDestinations,",");
		while (st.hasMoreTokens()) {
			String token=st.nextToken();
			destinationSet.add(token);			
		}
	}
	public void setDestinations(String[] newDestinations) {
		log.debug(getLogPrefix()+"calling setDestinations()");
		Set set=new HashSet();
		for (int i=0;i<newDestinations.length;i++) {
			log.debug(getLogPrefix()+"adding destination ["+newDestinations[i]+"]");
			set.add(newDestinations[i]); 
		}
		setDestinationSet(set);
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
			log.debug("size before retain all ["+destinationSet.size()+"]" );
			destinationSet.retainAll(newDestinations);
			log.debug("size after retain all ["+destinationSet.size()+"]" );
			destinationSet.addAll(newDestinations);
			log.debug("size after add all ["+destinationSet.size()+"]" );
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

	public void setGuardedObject(String string) {
		guardedObject = string;
	}
	public String getGuardedObject() {
		return guardedObject;
	}

	public void setRaised(boolean b) {
		raised = b;
	}
	public boolean isRaised() {
		return raised;
	}

	public void setSeverity(String severity) {
		setSeverityEnum((SeverityEnum)SeverityEnum.getEnumMap().get(severity));
	}
	public String getSeverity() {
		return severity==null?null:severity.getName();
	}
	public void setSeverityEnum(SeverityEnum enum) {
		severity = enum;
	}
	public SeverityEnum getSeverityEnum() {
		return severity;
	}

}
