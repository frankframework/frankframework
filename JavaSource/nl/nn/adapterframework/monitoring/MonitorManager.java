/*
 * $Log: MonitorManager.java,v $
 * Revision 1.4  2008-07-24 12:34:00  europe\L190409
 * rework
 *
 * Revision 1.3  2008/07/17 16:23:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed default monitors
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.nn.adapterframework.configuration.AttributeCheckingRule;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.Lock;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Manager for Monitoring.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class MonitorManager implements EventHandler {
	protected Logger log = LogUtil.getLogger(this);

	List monitors = new ArrayList();
	Map  eventsByThrower = new HashMap();
	Map  throwersByEvent = new HashMap();
	Map  eventNotificationListeners = new HashMap();
	
	Lock structureLock = new Lock();

	private static MonitorManager self=null;
	
	private Map destinations=new LinkedHashMap();
	
	private MonitorManager() {
	}

	public void configure() throws ConfigurationException {
//		IMonitorAdapter ma=MonitorAdapterFactory.getMonitorAdapter();
//		ma.setName("MCR (GALM)");
//		ma.register();
//		ma=new DummyMonitorAdapter();
//		ma.setName("dummy");
//		ma.register();
//		for (Iterator it=destinations.keySet().iterator(); it.hasNext();) {
//			String key=(String)it.next();
//			IMonitorAdapter monitorAdapter=getDestination(key);
//			monitorAdapter.configure();
//		}
//		getMonitor(0).setDestinationX("MCR (GALM)",false);
//		getMonitor(0).setDestinationX("dummy",true);
//		getMonitor(1).setDestinationX("MCR (GALM)",true);
//		getMonitor(1).setDestinationX("dummy",false);

//		if (monitors.size()==0) {
//			Monitor monitor=new Monitor();
//			monitor.setName("Configuration");
//			monitor.setExport(true);
//			monitor.setTypeEnum(EventTypeEnum.TECHNICAL);
//			monitor.setSeverityEnum(SeverityEnum.CRITICAL);
//			monitor.setGuardedObject("Receiver [FxfListener]");
////			monitor.setObject(((EventThrowing)throwersByEvent.get(ReceiverBase.RCV_CONFIGURATIONEXCEPTION_MONITOR_EVENT)).getEventSourceName());
//			Trigger alarm = new Trigger();
//			alarm.setEventCode(ReceiverBase.RCV_CONFIGURATIONEXCEPTION_MONITOR_EVENT);
//			alarm.setSeverityEnum(SeverityEnum.CRITICAL);
//			monitor.registerAlarm(alarm);
//			Trigger clearing = new Trigger();
//			clearing.setEventCode(ReceiverBase.RCV_CONFIGURED_MONITOR_EVENT);
//			monitor.registerClearing(clearing);
//		}
		for (Iterator it=destinations.keySet().iterator(); it.hasNext();) {
			String name=(String)it.next();
			IMonitorAdapter destination = getDestination(name);
			destination.configure();
		}
		for (Iterator it=monitors.iterator(); it.hasNext();) {
			Monitor monitor = (Monitor)it.next();
			monitor.configure();
		}
	}



	public void updateDestinations(String[] selectedDestinations) {
		Map monitorDestinations=new HashMap();
		log.debug("setting destinations selectedDestinations.length ["+selectedDestinations.length+"]");
		for (int i=0; i<selectedDestinations.length; i++) {
			String curSelectedDestination=selectedDestinations[i];
			log.debug("processing destination["+curSelectedDestination+"]");
			int pos=curSelectedDestination.indexOf(',');
			log.debug("pos["+pos+"]");
			int index=Integer.parseInt(curSelectedDestination.substring(0,pos));
			log.debug("index["+index+"]");
			Monitor monitor=getMonitor(index);
			String destination=curSelectedDestination.substring(pos+1);
			log.debug("destination["+destination+"]");
			Set monitorDestinationSet=(Set)monitorDestinations.get(monitor);
			if (monitorDestinationSet==null) {
				monitorDestinationSet=new HashSet();
				monitorDestinations.put(monitor,monitorDestinationSet);
			}
			monitorDestinationSet.add(destination);
		}
		for(int i=0;i<getMonitors().size();i++){
			Monitor monitor=getMonitor(i);
			Set monitorDestinationSet=(Set)monitorDestinations.get(monitor);
//			log.debug("setting ["+monitorDestinationSet.size()+"] destinations for monitor ["+monitor.getName()+"]");
			monitor.setDestinationSet(monitorDestinationSet);			
		}
	}


	public void setDigesterRules(Digester d) {
		Rule attributeChecker=new AttributeCheckingRule(); 

		d.addObjectCreate("*/destination","className",IMonitorAdapter.class);
		d.addSetProperties("*/destination");
		d.addSetTop("*/destination","register");
		d.addRule("*/destination", attributeChecker);

		d.addObjectCreate("*/monitor",Monitor.class);
		d.addSetProperties("*/monitor");
		d.addSetTop("*/monitor","register");
		d.addRule("*/monitor", attributeChecker);

		d.addObjectCreate("*/alarm",Trigger.class);
		d.addSetProperties("*/alarm");
		d.addSetNext("*/alarm","registerAlarm");
		d.addRule("*/alarm", attributeChecker);
		
		d.addObjectCreate("*/clearing",Trigger.class);
		d.addSetProperties("*/clearing");
		d.addSetNext("*/clearing","registerClearing");
		d.addRule("*/clearing", attributeChecker);
		
		d.addObjectCreate("*/trigger",Trigger.class);
		d.addSetProperties("*/trigger");
		d.addSetNext("*/trigger","registerTrigger");
		d.addRule("*/trigger", attributeChecker);

	}

	public void registerDestination(IMonitorAdapter monitorAdapter) {
		destinations.put(monitorAdapter.getName(),monitorAdapter);	
	}
	public IMonitorAdapter getDestination(String name) {
		return (IMonitorAdapter)destinations.get(name);
	}
	public Map getDestinations() {
		return destinations;
	}
	
	public synchronized static MonitorManager getInstance() {
		if (self==null) {
			self=new MonitorManager();
		}
		return self;
	}

	public static EventHandler getEventHandler() {
		return getInstance();
	}


	public void addMonitor(Monitor monitor) {
		monitor.setOwner(this);
		try {
			structureLock.acquireExclusive();
			try {
				monitors.add(monitor);
			} finally {
				structureLock.releaseExclusive();
			}
		} catch (InterruptedException e) {
			log.error("Could not obtain lock for addMonitor" , e);
		}
	}
	public Monitor removeMonitor(int index) {
		Monitor result=null;
		try {
			structureLock.acquireExclusive();
			try {
				result = (Monitor)monitors.remove(index);
			} finally {
				structureLock.releaseExclusive();
			}
		} catch (InterruptedException e) {
			log.error("Could not obtain lock for removeMonitor" , e);
		}
		return result;
	}
	public Monitor getMonitor(int index) {
		try {
			structureLock.acquireShared();
			try {
				return (Monitor)monitors.get(index);
			} finally {
				structureLock.releaseShared();
			}
		} catch (InterruptedException e) {
			log.error("Could not obtain lock for getMonitor" , e);
		}
		return null;
	}
	public Monitor findMonitor(String name) {
		try {
			structureLock.acquireShared();
			try {
				for (int i=0; i<monitors.size(); i++) {
					Monitor monitor = (Monitor)monitors.get(i);
					if (name!=null && name.equals(monitor.getName()) || name==monitor.getName()) {
						return monitor;
					}
				}
			} finally {
				structureLock.releaseShared();
			}
		} catch (InterruptedException e) {
			log.error("Could not obtain lock for findMonitor" , e);
		}
		return null;
	}

	public List getMonitors() {
		return monitors;
	}

	public List getEventCodes(EventThrowing thrower) {
		if (thrower!=null) {
			return (List)eventsByThrower.get(thrower);
		} else {
			List result = new ArrayList();
			for (Iterator it=throwersByEvent.keySet().iterator(); it.hasNext();) {
				result.add(it.next());
			}
			return result;
		}
	}

	public void registerEvent(EventThrowing thrower, String eventCode) {
		try {
			structureLock.acquireExclusive();
			try {
				List throwersEvents = (List)eventsByThrower.get(thrower);
				if (throwersEvents==null) {
					throwersEvents = new ArrayList();
					eventsByThrower.put(thrower, throwersEvents);
				}
				throwersEvents.add(eventCode);
				List eventsThrowers = (List)throwersByEvent.get(eventCode);
				if (eventsThrowers==null) {
					eventsThrowers = new ArrayList();
					throwersByEvent.put(eventCode, eventsThrowers);
				}
				eventsThrowers.add(thrower);
			} finally {
				structureLock.releaseExclusive();
			}
		} catch (InterruptedException e) {
			log.error("Could not obtain lock for registerEvent" , e);
		}
	}

	public void registerEventNotificationListener(Trigger trigger, String eventCode, String thrower) throws MonitorException {
		EventThrowing target=null; 	
		if (StringUtils.isNotEmpty(thrower)) {
			for (Iterator it=eventsByThrower.keySet().iterator(); target==null && it.hasNext();) {
				EventThrowing candidate = (EventThrowing)it.next();
				if (candidate.getEventSourceName().equals(thrower)) {
					target=candidate;
				}
			}
			if (target==null) {
				throw new MonitorException("cannot find thrower ["+thrower+"]");
			}
		}
		registerEventNotificationListener(trigger,eventCode,target);
	}
	
	public void registerEventNotificationListener(Trigger trigger, String eventCode, EventThrowing thrower) throws MonitorException {
		Map notificationListeners = (Map)eventNotificationListeners.get(eventCode);
		if (notificationListeners==null) {
			notificationListeners=new LinkedHashMap();
			eventNotificationListeners.put(eventCode,notificationListeners);
		}
		if (notificationListeners.containsKey(trigger)) {
			throw new MonitorException("eventcode ["+eventCode+"] already registerd for Trigger ["+trigger+"]");
		}
		notificationListeners.put(trigger,thrower);
	}

	public synchronized void fireEvent(EventThrowing source, String eventCode) {
		try {
			structureLock.acquireShared();
			try {
				Map notificationListeners = (Map)eventNotificationListeners.get(eventCode);
				if (notificationListeners!=null) {
					for (Iterator it=notificationListeners.keySet().iterator(); it.hasNext();) {
						Trigger trigger = (Trigger)it.next();
						EventThrowing filter = (EventThrowing)notificationListeners.get(trigger);
						if (filter==null || filter==source) {
							try {
								trigger.evaluateEvent(source,eventCode);
							} catch (MonitorException e) {
								log.error("Could not evaluate event ["+eventCode+"]",e);
							}
						}
					}
				}
			} finally {
				structureLock.releaseShared();
			}
		} catch (InterruptedException e) {
			log.error("Could not obtain lock for fireEvent" , e);
		}
	}

	
	public XmlBuilder toXml(boolean configOnly) {
		XmlBuilder configXml=new XmlBuilder("monitoring");
		XmlBuilder destinationsXml=new XmlBuilder("destinations");
		for (Iterator it=destinations.keySet().iterator(); it.hasNext(); ) {
			String name=(String)it.next();
			IMonitorAdapter ma=getDestination(name);
			XmlBuilder destinationXml=new XmlBuilder("destination");
			destinationXml.addAttribute("name",ma.getName());
			destinationXml.addAttribute("className",ma.getClass().getName());
			destinationsXml.addSubElement(destinationXml);
		}
		configXml.addSubElement(destinationsXml);
		XmlBuilder monitorsXml=new XmlBuilder("monitors");
		for (int i=0; i<monitors.size(); i++) {
			Monitor monitor=getMonitor(i);
			monitorsXml.addSubElement(monitor.toXml(i,configOnly));
		}
		configXml.addSubElement(monitorsXml);
		
//		if (!configOnly) {
//			XmlBuilder throwersXml=new XmlBuilder("throwers");
//			for (Iterator it=eventsByThrower.keySet().iterator();it.hasNext();) {
//				XmlBuilder throwerXml=new XmlBuilder("thrower");
//				EventThrowing thrower = (EventThrowing)it.next();
//				throwerXml.addAttribute("name",thrower.getEventSourceName());
//				throwersXml.addSubElement(throwerXml);
//			}
//			configXml.addSubElement(throwersXml);
//		
//			XmlBuilder eventsXml=new XmlBuilder("events");
//			for (Iterator it=throwersByEvent.keySet().iterator();it.hasNext();) {
//				XmlBuilder eventXml=new XmlBuilder("event");
//				String event = (String)it.next();
//				eventXml.addAttribute("name",event);
//				eventsXml.addSubElement(eventXml);
//			}
//			configXml.addSubElement(eventsXml);
//		}
		return configXml;
	}
	
	public List getThrowers() {
		List result = new ArrayList(eventsByThrower.keySet());
		return result;
	}
	public Iterator getThrowerIterator() {
		return eventsByThrower.keySet().iterator();
	}

	public EventThrowing findThrower(String thrower) {
		for(Iterator it=getThrowerIterator(); it.hasNext();) {
			EventThrowing candidate = (EventThrowing)it.next();
			if (candidate.getEventSourceName().equals(thrower)) {
				return candidate;
			}
		}
		return null;
	}

}
