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
 * @version $Id$
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
