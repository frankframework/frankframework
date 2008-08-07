/*
 * $Log: IMonitorAdapter.java,v $
 * Revision 1.4  2008-08-07 11:31:27  europe\L190409
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
