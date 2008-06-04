/*
 * $Log: IMonitorAdapter.java,v $
 * Revision 1.1.10.1  2008-06-04 16:23:40  europe\L190409
 * sync from HEAD
 *
 * Revision 1.2  2008/05/21 10:52:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified monitorAdapter interface
 *
 * Revision 1.1  2007/09/27 12:55:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
	
	void fireEvent(String subSource, EventTypeEnum eventType, SeverityEnum severity, String message, Throwable t); 

}
