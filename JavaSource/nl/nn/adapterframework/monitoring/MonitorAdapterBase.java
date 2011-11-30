/*
 * $Log: MonitorAdapterBase.java,v $
 * Revision 1.4  2011-11-30 13:51:43  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2008/08/07 11:31:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework
 *
 * Revision 1.1  2008/07/24 12:34:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework
 *
 */
package nl.nn.adapterframework.monitoring;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public abstract class MonitorAdapterBase implements IMonitorAdapter {
	protected Logger log = LogUtil.getLogger(this);

	private String SOURCE_ID_KEY="galm.source";

	private String name;
	private String hostname;
	private String sourceId;

	public MonitorAdapterBase() {
		super();
		log.debug("creating Destination ["+ClassUtils.nameOf(this)+"]");
	}

	
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getName())) {
			setName(ClassUtils.nameOf(this));
		}
		hostname=Misc.getHostname();
		AppConstants appConstants = AppConstants.getInstance();
		sourceId=appConstants.getResolvedProperty(SOURCE_ID_KEY);
		if (StringUtils.isEmpty(sourceId)) {
			throw new ConfigurationException("cannot read sourceId from ["+SOURCE_ID_KEY+"]");
		}
	}

	public String makeXml(String subSource, EventTypeEnum eventType, SeverityEnum severity, String message, Throwable t) {
		XmlBuilder eventXml = new XmlBuilder("event");
		eventXml.addAttribute("hostname",hostname);		
		eventXml.addAttribute("source",sourceId);		
		eventXml.addAttribute("subSource",subSource);		
		eventXml.addAttribute("eventType",eventType.getName());		
		eventXml.addAttribute("severity",severity.getName());		
		eventXml.addAttribute("message",message);		
		return eventXml.toXML();
	}

	public XmlBuilder toXml() {
		XmlBuilder destinationXml=new XmlBuilder("destination");
		destinationXml.addAttribute("name",getName());
		destinationXml.addAttribute("className",getClass().getName());
		return destinationXml;
	}


	public void setName(String string) {
		name = string;
	}
	public String getName() {
		return name;
	}

	public void register(Object x) {
		MonitorManager.getInstance().registerDestination(this);
	}

}
