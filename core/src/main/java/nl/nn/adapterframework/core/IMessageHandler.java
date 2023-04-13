/*
   Copyright 2013 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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

import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.stream.Message;

/**
 * Interface that {@link IPushingListener PushingListeners} can use to handle the messages they receive.
 * A call to any of the method defined in this interface will do to process the message.
 * @param <M> the raw message type
 *
 * @author  Gerrit van Brakel
 * @since   4.2
 */
public interface IMessageHandler<M> {

	/**
	 * Will use listener to perform getIdFromRawMessage(), getStringFromRawMessage and afterMessageProcessed
	 */
	public void processRawMessage(IListener<M> origin, RawMessageWrapper<M> message, PipeLineSession session, boolean duplicatesAlreadyChecked) throws ListenerException;

	/**
	 * Same as {@link #processRawMessage(IListener,RawMessageWrapper,PipeLineSession, boolean)}, but now updates IdleStatistics too
	 */
	public void processRawMessage(IListener<M> origin, RawMessageWrapper<M> message, PipeLineSession session, long waitingTime, boolean duplicatesAlreadyChecked) throws ListenerException;

	/**
	 * Alternative to functions above, will NOT use getIdFromRawMessage() and getStringFromRawMessage(). Used by PushingListeners.
	 */
	public Message processRequest(IListener<M> origin, RawMessageWrapper<M> rawMessage, Message message, PipeLineSession session) throws ListenerException;

	/**
	 *	Formats any exception thrown by any of the above methods to a message that can be returned.
	 *  Can be used if the calling system has no other way of returning the exception to the caller.
	 */
	public Message formatException(String extraInfo, String correlationId, Message message, Throwable t);
}
