/*
 * $Log: IMessageHandler.java,v $
 * Revision 1.1  2004-08-03 13:11:49  L190409
 * first version
 *
 */
package nl.nn.adapterframework.core;

import java.util.HashMap;

/**
 * Interface that {@link IPushingListener PushingListeners} can use to handle the messages they receive.
 *
 * @version Id
 * @author Gerrit van Brakel
 * @since 4.2
 */
public interface IMessageHandler {
	public static final String version="$Id: IMessageHandler.java,v 1.1 2004-08-03 13:11:49 L190409 Exp $";
	
	/**
	 * Will use listener to perform getIdFromRawMessage(), getStringFromRawMessage and afterMessageProcessed 
	 * @param message
	 */
	public void processRawMessage(IListener origin, Object message, HashMap context) throws ListenerException;
	
	/**
	 * Same as {@link processRequest(Object)}, but now updates IdleStatistics too
	 */
	public void processRawMessage(IListener origin, Object message, HashMap context, long waitingTime) throws ListenerException;
	
	/**
	 * Alternative to functions above, wil NOT use getIdFromRawMessage(), getStringFromRawMessage and afterMessageProcessed
	 * @param message
	 * @return
	 */
	public String processRequest(String message);
	/**
	 * Does a processRequest with a correlationId from the client. This is usefull for logging purposes,
	 * as the correlationId is logged also.
	 * @since 4.0
	 */	
	public String processRequest(String correlationId, String message);

}
