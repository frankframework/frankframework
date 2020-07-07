/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.stream.Message;

/**
 * Base-interface for IPullingListener and IPushingListener.
 * @param <M> the raw message type 
 * 
 * @author  Gerrit van Brakel
 * @since   4.2
 */
public interface IListener<M> extends INamedObject {

	/**
	 * <code>configure()</code> is called once at startup of the framework in the <code>configure()</code> method 
	 * of the owner of this listener. 
	 * Purpose of this method is to reduce creating connections to databases etc. in the {@link nl.nn.adapterframework.core.IPullingListener#getRawMessage(Map)} method.
	 * As much as possible class-instantiating should take place in the
	 * <code>configure()</code> or <code>open()</code> method, to improve performance.
	 */ 
	public void configure() throws ConfigurationException;
	
	/**
	 * Prepares the listener for receiving messages.
	 * <code>open()</code> is called once each time the listener is started.
	 */
	void open() throws ListenerException;
	
	/**
	 * Close all resources used for listening.
	 * Called once once each time the listener is stopped.
	 */
	void close() throws ListenerException;
	
	/**
	 * Extracts ID-string from message obtained from {@link nl.nn.adapterframework.core.IPullingListener#getRawMessage(Map)}. May also extract
	 * other parameters from the message and put those in the context.
	 * <br>
	 * Common entries in the session context are:
	 * <ul>
	 * 	<li>id: messageId, identifies the current transportation of the message</li>
	 * 	<li>cid: correlationId, identifies the processing of the message in the global chain</li>
	 * 	<li>tsReceived: timestamp of reception of the message, formatted as yyyy-MM-dd HH:mm:ss.SSS</li>
	 * 	<li>tsSent: timestamp of sending of the message (only when available), formatted as yyyy-MM-dd HH:mm:ss.SSS</li>
	 * </ul>
	 * 
	 * @return Correlation ID string.
	 */
	String getIdFromRawMessage(M rawMessage, Map<String,Object> context) throws ListenerException;
	
	/**
	 * Extracts string from message obtained from {@link nl.nn.adapterframework.core.IPullingListener#getRawMessage(Map)}. May also extract
	 * other parameters from the message and put those in the threadContext.
	 * @return input message for adapter.
	 */
	Message extractMessage(M rawMessage, Map<String,Object> context) throws ListenerException;
	
	/**
	 * Called to perform actions (like committing or sending a reply) after a message has been processed by the 
	 * Pipeline. 
	 */
	void afterMessageProcessed(PipeLineResult processResult, Object rawMessageOrWrapper, Map<String,Object> context) throws ListenerException;

}
