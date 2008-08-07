/*
 * $Log: Trigger.java,v $
 * Revision 1.4  2008-08-07 11:31:27  europe\L190409
 * rework
 *
 * Revision 1.3  2008/07/24 12:34:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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

import java.util.Date;
import java.util.LinkedList;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

/**
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class Trigger {
	protected Logger log = LogUtil.getLogger(this);
	
	private Monitor owner;
	private SeverityEnum severity;
	private boolean alarm;

	private String eventCode;
	private String source;
		
	private int threshold=0;
	private int period=0;
	
	private LinkedList eventDts=null;
		

	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(eventCode)) {
			log.warn("trigger of Monitor ["+owner.getName()+"] should have eventCode specified");
		}
		if (StringUtils.isNotEmpty(eventCode)) {
			try {
				getOwner().registerEventNotificationListener(this,eventCode,source);
			} catch (MonitorException e) {
				throw new ConfigurationException(e);
			}
		}
		if (threshold>0) {
			if (eventDts==null) {
				eventDts = new LinkedList();
			}
		} else {
			eventDts=null;
		}
	}
	

	public void evaluateEvent(EventThrowing source, String eventCode) throws MonitorException {
		if (getThreshold()>0) {
			Date now = new Date();
			cleanUpEvents(now);
			eventDts.add(now);
			if (eventDts.size()>=getThreshold()) {
				getOwner().changeState(alarm, getSeverityEnum(), source, eventCode, null);
			}
		} else {
			getOwner().changeState(alarm, getSeverityEnum(), source, eventCode, null);
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
		if (StringUtils.isNotEmpty(getSource())) {
			trigger.addAttribute("source",getSource());
		}
		if (getSeverity()!=null) {
			trigger.addAttribute("severity",getSeverity());
		}
		if (getThreshold()>0) {
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

	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}


	public void setAlarm(boolean b) {
		alarm = b;
	}
	public boolean isAlarm() {
		return alarm;
	}

	public String getType() {
		if (isAlarm()) {
			return "Alarm";
		} else {
			return "Clearing";
		}
	}
	public void setType(String type) {
		if (type.equalsIgnoreCase("Alarm")) {
			setAlarm(true);
		}
		if (type.equalsIgnoreCase("Clearing")) {
			setAlarm(false);
		}
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
	public SeverityEnum getSeverityEnum() {
		return severity;
	}
	public String getSeverity() {
		return severity==null?null:severity.getName();
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

	public void setSource(String source) {
		this.source = source;
	}
	public String getSource() {
		return source;
	}

}
