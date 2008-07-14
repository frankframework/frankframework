/*
 * $Log: Trigger.java,v $
 * Revision 1.1  2008-07-14 17:21:18  europe\L190409
 * first version of flexible monitoring
 *
 */
package nl.nn.adapterframework.monitoring;

import java.util.Date;
import java.util.LinkedList;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.lang.StringUtils;

/**
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class Trigger {
	
	private Monitor owner;
	private SeverityEnum severity;
	private boolean alarm;
	private String eventCode;
	
	private int threshold=1;
	private int period=0;
	
	private LinkedList eventDts=null;
		

	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(eventCode)) {
			throw new ConfigurationException("trigger of Monitor ["+owner.getName()+"] must have eventCode specified");
		}
		if (StringUtils.isNotEmpty(eventCode)) {
			try {
				getOwner().registerEventNotificationListener(this,eventCode);
			} catch (MonitorException e) {
				throw new ConfigurationException(e);
			}
		}
		if (threshold>1) {
			if (eventDts==null) {
				eventDts = new LinkedList();
			}
		} else {
			eventDts=null;
		}
	}
	

	public void evaluateEvent(EventThrowing source, String eventCode) throws MonitorException {
		if (getThreshold()>1) {
			Date now = new Date();
			cleanUpEvents(now);
			eventDts.add(now);
			if (eventDts.size()>=getThreshold()) {
				getOwner().changeState(alarm, getSeverity(), source, eventCode, null);
			}
		} else {
			getOwner().changeState(alarm, getSeverity(), source, eventCode, null);
		}
	}	
	
	public void notificationOfReverseTrigger(EventThrowing source) {
		if (eventDts!=null) {
			eventDts.clear();
		}
	}

	protected void cleanUpEvents(Date now) {
		Date firstDate = (Date)eventDts.getFirst();
		if ((now.getTime()-firstDate.getTime())>getPeriod()*1000) {
			eventDts.removeFirst();
		}
	}

	public void toXml(XmlBuilder monitor) {
		XmlBuilder trigger=new XmlBuilder(isAlarm()?"alarm":"clearing");
		monitor.addSubElement(trigger);
		trigger.addAttribute("eventCode",getEventCode());
		if (getSeverity()!=null) {
			trigger.addAttribute("severity",getSeverity().getName());
		}
		if (getThreshold()>1) {
			trigger.addAttribute("threshold",getThreshold());
		}
		if (getPeriod()>0) {
			trigger.addAttribute("period",getPeriod());
		}
	}

	public void setOwner(Monitor monitor) {
		owner = monitor;
	}
	public Monitor getOwner() {
		return owner;
	}


	public void setAlarm(boolean b) {
		alarm = b;
	}
	public boolean isAlarm() {
		return alarm;
	}

	public void setEventCode(String string) {
		eventCode = string;
	}
	public String getEventCode() {
		return eventCode;
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

	public void setThreshold(int i) {
		threshold = i;
	}
	public int getThreshold() {
		return threshold;
	}

	public void setPeriod(int i) {
		period = i;
	}
	public int getPeriod() {
		return period;
	}

}
