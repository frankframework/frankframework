/*
 * $Log: EventThrowing.java,v $
 * Revision 1.4  2011-11-30 13:51:43  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2009/05/13 08:18:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved monitoring: triggers can now be filtered multiselectable on adapterlevel
 *
 * Revision 1.1  2008/07/14 17:21:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version of flexible monitoring
 *
 */
package nl.nn.adapterframework.monitoring;

import nl.nn.adapterframework.core.IAdapter;

/**
 * Interface to be implemented by objects to be monitored; Is called by code that handles event.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public interface EventThrowing {
	
	public String getEventSourceName();
	public IAdapter getAdapter();
}
