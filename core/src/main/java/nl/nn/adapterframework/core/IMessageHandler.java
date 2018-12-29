/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.core;

import java.util.Map;

/**
 * Interface that {@link IPushingListener PushingListeners} can use to handle the messages they receive.
 * A call to any of the method defined in this interface will do to process the message.
 *
 * @author  Gerrit van Brakel
 * @since   4.2
 */
public interface IMessageHandler {
	
	/**
	 * Will use listener to perform getIdFromRawMessage(), getStringFromRawMessage and afterMessageProcessed
	 * @param origin the source
	 * @param message the message to be processed
	 * @param context the context in which the message resides
	 * @throws ListenerException thrown when listening to the message fails
	 */
	public void processRawMessage(IListener origin, Object message, Map<String,Object> context) throws ListenerException;
	
	/**
	 * Same as {@link #processRawMessage(IListener,Object,Map)}, but now updates IdleStatistics too
	 * @param origin the source
	 * @param message the message to be processed
	 * @param context the context in which the message resides
	 * @param waitingTime the waiting time
	 * @throws ListenerException thrown when listening to the message fails
	 */
	public void processRawMessage(IListener origin, Object message, Map<String,Object> context, long waitingTime) throws ListenerException;

	/**
	 * Same as {@link #processRawMessage(IListener,Object,Map)}, but now without context, for convenience
	 * @param origin the source
	 * @param message the message to be processed
	 * @throws ListenerException thrown when listening to the message fails
	 */
	public void processRawMessage(IListener origin, Object message) throws ListenerException;
	
	/**
	 * Alternative to functions above, wil NOT use getIdFromRawMessage() and getStringFromRawMessage().
	 * @param origin the source
	 * @param message holds the request
	 * @return the request made
	 * @throws ListenerException thrown when listening to the message (request) fails
	 */
	public String processRequest(IListener origin, String message) throws ListenerException;

	/**
	 * Does a processRequest() with a correlationId from the client. This is useful for logging purposes,
	 * as the correlationId is logged also.
	 * @param origin the source
	 * @param correlationId the id from the client
	 * @param message holds the request
	 * @return the request made
	 * @throws ListenerException thrown when listening to the message (request) fails
	 */	
	public String processRequest(IListener origin, String correlationId, String message) throws ListenerException;
	public String processRequest(IListener origin, String correlationId, String message, Map<String,Object> context) throws ListenerException;
	public String processRequest(IListener origin, String correlationId, String message, Map<String,Object> context, long waitingTime) throws ListenerException;

	/**
	 *	Formats any exception thrown by any of the above methods to a message that can be returned.
	 *  Can be used if the calling system has no other way of returning the exception to the caller.
	 *  @param extrainfo extra information concerning the exception
	 *  @param correlationId the id of the request that went wrong
	 *  @param message the message
	 *  @param t the exception that is thrown
	 *  @return a well formed message on why the exception occurred
	 */	
	public String formatException(String extrainfo, String correlationId, String message, Throwable t);

}
