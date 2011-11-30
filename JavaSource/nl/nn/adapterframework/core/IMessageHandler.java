/*
 * $Log: IMessageHandler.java,v $
 * Revision 1.7  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.5  2007/10/03 08:10:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map
 *
 * Revision 1.4  2005/07/05 12:54:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * allow to set parameters from context for processRequest() methods
 *
 * Revision 1.3  2004/08/23 13:07:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated JavaDoc
 *
 * Revision 1.2  2004/08/09 14:00:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduced formatErrorMessage
 * some interface-changes
 *
 * Revision 1.1  2004/08/03 13:11:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.core;

import java.util.Map;

/**
 * Interface that {@link IPushingListener PushingListeners} can use to handle the messages they receive.
 * A call to any of the method defined in this interface will do to process the message.
 *
 * @author  Gerrit van Brakel
 * @since   4.2
 * @version Id
 */
public interface IMessageHandler {
	public static final String version = "$RCSfile: IMessageHandler.java,v $ $Revision: 1.7 $ $Date: 2011-11-30 13:51:55 $";
	
	/**
	 * Will use listener to perform getIdFromRawMessage(), getStringFromRawMessage and afterMessageProcessed 
	 */
	public void processRawMessage(IListener origin, Object message, Map context) throws ListenerException;
	
	/**
	 * Same as {@link #processRawMessage(IListener,Object,Map)}, but now updates IdleStatistics too
	 */
	public void processRawMessage(IListener origin, Object message, Map context, long waitingTime) throws ListenerException;

	/**
	 * Same as {@link #processRawMessage(IListener,Object,Map)}, but now without context, for convenience
	 */
	public void processRawMessage(IListener origin, Object message) throws ListenerException;
	
	/**
	 * Alternative to functions above, wil NOT use getIdFromRawMessage() and getStringFromRawMessage().
	 */
	public String processRequest(IListener origin, String message) throws ListenerException;

	/**
	 * Does a processRequest() with a correlationId from the client. This is usefull for logging purposes,
	 * as the correlationId is logged also.
	 */	
	public String processRequest(IListener origin, String correlationId, String message) throws ListenerException;
	public String processRequest(IListener origin, String correlationId, String message, Map context) throws ListenerException;
	public String processRequest(IListener origin, String correlationId, String message, Map context, long waitingTime) throws ListenerException;

	/**
	 *	Formats any exception thrown by any of the above methods to a message that can be returned.
	 *  Can be used if the calling system has no other way of returnin the exception to the caller. 
	 */	
	public String formatException(String extrainfo, String correlationId, String message, Throwable t);

}
