/*
 * $Log: IMessageHandler.java,v $
 * Revision 1.2  2004-08-09 14:00:02  L190409
 * introduced formatErrorMessage
 * some interface-changes
 *
 * Revision 1.1  2004/08/03 13:11:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.core;

import java.util.HashMap;

/**
 * Interface that {@link IPushingListener PushingListeners} can use to handle the messages they receive.
 * A call to any of the method defined in this interface will do to process the message.
 *
 * @version Id
 * @author Gerrit van Brakel
 * @since 4.2
 */
public interface IMessageHandler {
	public static final String version="$Id: IMessageHandler.java,v 1.2 2004-08-09 14:00:02 L190409 Exp $";
	
	/**
	 * Will use listener to perform getIdFromRawMessage(), getStringFromRawMessage and afterMessageProcessed 
	 */
	public void processRawMessage(IListener origin, Object message, HashMap context) throws ListenerException;
	
	/**
	 * Same as {@link processRequest(IListener,Object,HashMap)}, but now updates IdleStatistics too
	 */
	public void processRawMessage(IListener origin, Object message, HashMap context, long waitingTime) throws ListenerException;

	/**
	 * Same as {@link processRequest(IListener,Object,HashMap)}, but now without context, for convenience
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

	/**
	 *	Formats any exception thrown by any of the above methods to a message that can be returned.
	 *  Can be used if the calling system has no other way of returnin the exception to the caller. 
	 */	
	public String formatException(String extrainfo, String correlationId, String message, Throwable t);

}
