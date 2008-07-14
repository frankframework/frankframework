/*
 * $Log: EventHandler.java,v $
 * Revision 1.1  2008-07-14 17:21:18  europe\L190409
 * first version of flexible monitoring
 *
 */
package nl.nn.adapterframework.monitoring;

/**
 * Interface exposed to objects to be monitored, to throw their events to; To be implemented by code that handles event.
 *  
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public interface EventHandler {
	
	public void registerEvent(EventThrowing source, String eventCode);
	public void fireEvent(EventThrowing source, String eventCode);

}
