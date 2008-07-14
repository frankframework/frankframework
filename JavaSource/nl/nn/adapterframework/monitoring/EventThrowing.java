/*
 * $Log: EventThrowing.java,v $
 * Revision 1.1  2008-07-14 17:21:18  europe\L190409
 * first version of flexible monitoring
 *
 */
package nl.nn.adapterframework.monitoring;

/**
 * Interface to be implemented by objects to be monitored; Is called by code that handles event.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public interface EventThrowing {
	
	public String getEventSourceName();

}
