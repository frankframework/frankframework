/*
 * $Log: EventThrowing.java,v $
 * Revision 1.2  2009-05-13 08:18:50  L190409
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
