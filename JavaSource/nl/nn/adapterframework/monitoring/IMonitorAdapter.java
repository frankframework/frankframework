/*
 * $Log: IMonitorAdapter.java,v $
 * Revision 1.1  2007-09-27 12:55:41  europe\L190409
 * introduction of monitoring
 *
 */
package nl.nn.adapterframework.monitoring;

/**
 * Interface to monitoring service. 
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public interface IMonitorAdapter {
	
	void fireEvent(String subSource, EventTypeEnum eventType, SeverityEnum severity, String message); 

}
