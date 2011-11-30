/*
 * $Log: IMonitorAdapter.java,v $
 * Revision 1.6  2011-11-30 13:51:43  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2008/08/07 11:31:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework
 *
 * Revision 1.3  2008/07/24 12:34:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework
 *
 * Revision 1.2  2008/05/21 10:52:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified monitorAdapter interface
 *
 * Revision 1.1  2007/09/27 12:55:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of monitoring
 *
 */
package nl.nn.adapterframework.monitoring;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Interface to monitoring service. 
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public interface IMonitorAdapter {

	void configure() throws ConfigurationException;
	
	void fireEvent(String subSource, EventTypeEnum eventType, SeverityEnum severity, String message, Throwable t); 

	void register(Object x);
	public XmlBuilder toXml();
	
	void setName(String name);	
	String getName();
}
