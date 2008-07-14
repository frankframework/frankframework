/*
 * $Log: Monitor.java,v $
 * Revision 1.1  2008-07-14 17:21:18  europe\L190409
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
	private boolean raised=false;
	private boolean functional=false;
	private SeverityEnum severity=null;  
	private boolean export=true;
	private String object;
	
	private MonitorManager owner=null;
	private EventTypeEnum eventType;

	private List triggers = new ArrayList();
	
	public Monitor() {
		super();
	}

	public void register(Object x) {
		MonitorManager.getInstance().addMonitor(this);
	}
	
	public void configure() throws ConfigurationException {
		eventType = functional?EventTypeEnum.FUNCTIONAL:EventTypeEnum.TECHNICAL;
		for (Iterator it=triggers.iterator(); it.hasNext();) {
			Trigger trigger = (Trigger)it.next();
			trigger.configure();
		}
	}
	
	public void registerEventNotificationListener(Trigger trigger, String eventCode) throws MonitorException {
		getOwner().registerEventNotificationListener(trigger,eventCode,object);
	}
	
	public void changeState(boolean alarm, SeverityEnum severity, EventThrowing source, String details, Throwable t) throws MonitorException {
		if (export) {
			boolean up=alarm && (!raised || this.severity==null || this.severity.compareTo(severity)<=0);
			boolean clear=raised && (!alarm || up && this.severity!=null && this.severity!=severity);
			if (clear) {
				SeverityEnum clearSeverity=this.severity!=null?this.severity:severity;
				getOwner().changeMonitorState(source, EventTypeEnum.CLEARING, clearSeverity, details, t);
			}
			if (up) {
				getOwner().changeMonitorState(source, eventType, severity, details, t);
			}
		}
		raised=alarm;
		this.severity=severity;
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

	public void toXml(XmlBuilder config, int index) {
		XmlBuilder monitor=new XmlBuilder("monitor");
		config.addSubElement(monitor);
		monitor.addAttribute("name",getName());
		if (index>=0) {
			monitor.addAttribute("index",index);
		}
		monitor.addAttribute("functional",isFunctional());
		if (getEventType()!=null) {
			monitor.addAttribute("eventType",getEventType().getName());
		}
		if (getSeverity()!=null) {
			monitor.addAttribute("severity",getSeverity().getName());
		}
		monitor.addAttribute("raised",isRaised());
		monitor.addAttribute("export",isExport());
		monitor.addAttribute("object",getObject());
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


	public void setName(String string) {
		name = string;
	}
	public String getName() {
		return name;
	}

	public void setExport(boolean b) {
		export = b;
	}
	public boolean isExport() {
		return export;
	}

	public void setFunctional(boolean b) {
		functional = b;
	}
	public boolean isFunctional() {
		return functional;
	}


	public void setRaised(boolean b) {
		raised = b;
	}
	public boolean isRaised() {
		return raised;
	}

	public void setEventType(String eventType) {
		setEventTypeEnum((EventTypeEnum)EventTypeEnum.getEnumMap().get(eventType));
	}
	public void setEventTypeEnum(EventTypeEnum enum) {
		eventType = enum;
	}
	public EventTypeEnum getEventType() {
		return eventType;
	}

	public void setObject(String string) {
		object = string;
	}
	public String getObject() {
		return object;
	}

	public void setSeverity(String severity) {
		setSeverityEnum((SeverityEnum)SeverityEnum.getEnumMap().get(severity));
	}
	public void setSeverityEnum(SeverityEnum enum) {
		severity = enum;
	}
	public SeverityEnum getSeverity() {
		return severity;
	}

}
