/*
 * $Log: Monitor.java,v $
 * Revision 1.2  2008-07-17 16:17:19  europe\L190409
 * rework
 *
 * Revision 1.1  2008/07/14 17:21:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version of flexible monitoring
 *
 */
package nl.nn.adapterframework.monitoring;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class Monitor {

	private String name;
	private EventTypeEnum type=EventTypeEnum.TECHNICAL;
	private String guardedObject;
	private boolean export=true;
	private boolean raised=false;
	private SeverityEnum severity=null;  

	
	private MonitorManager owner=null;

	private List triggers = new ArrayList();
	
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
		if (export) {
			boolean up=alarm && (!raised || getSeverityEnum()==null || getSeverityEnum().compareTo(severity)<=0);
			boolean clear=raised && (!alarm || up && getSeverityEnum()!=null && getSeverityEnum()!=severity);
			if (clear) {
				SeverityEnum clearSeverity=getSeverityEnum()!=null?getSeverityEnum():severity;
				getOwner().changeMonitorState(source, EventTypeEnum.CLEARING, clearSeverity, details, t);
			}
			if (up) {
				getOwner().changeMonitorState(source, getTypeEnum(), severity, details, t);
			}
		}
		raised=alarm;
		setSeverityEnum(severity);
		notifyReverseTrigger(alarm,source);
	}

	protected void notifyReverseTrigger(boolean alarm, EventThrowing source) {
		for (Iterator it=triggers.iterator(); it.hasNext();) {
			Trigger trigger=(Trigger)it.next();
			if (trigger.isAlarm()!=alarm) {
				trigger.notificationOfReverseTrigger(source);
			}
		}
	}

	public void toXml(XmlBuilder config, int index, boolean configOnly) {
		XmlBuilder monitor=new XmlBuilder("monitor");
		config.addSubElement(monitor);
		if (index>=0) {
			monitor.addAttribute("index",index);
		}
		monitor.addAttribute("name",getName());
		monitor.addAttribute("type",getType());
		monitor.addAttribute("guardedObject",getGuardedObject());
		monitor.addAttribute("export",isExport());
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

	public void setExport(boolean b) {
		export = b;
	}
	public boolean isExport() {
		return export;
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
