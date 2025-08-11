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
package org.frankframework.core;

import org.frankframework.receivers.RawMessageWrapper;

/**
 * Defines listening behaviour of message driven receivers.
 * @param <M> the raw message type
 *
 * @author Gerrit van Brakel
 * @since 4.2
 */
public interface IPushingListener<M> extends IListener<M> {

	/**
	 * Set the handler that will do the processing of the message.
	 * Each of the received messages must be pushed through handler.processMessage()
	 */
	void setHandler(IMessageHandler<M> handler);

	/**
	 * Set a (single) listener that will be notified of any exceptions.
	 * The listener should use this listener to notify the receiver of
	 * any exception that occurs outside the processing of a message.
	 */
	void setExceptionListener(IbisExceptionListener listener);

	/**
	 * Wrap a raw message in a MessageWrapper. Populate {@link PipeLineSession} with properties
	 * from the message.
	 *
	 * @param rawMessage The raw message data, unwrapped
	 * @param session {@link PipeLineSession} to populate with properties from the message.
	 * @return Wrapped raw message
	 * @throws ListenerException If any exception occurs during wrapping, a {@link ListenerException} is thrown.
	 */
	RawMessageWrapper<M> wrapRawMessage(M rawMessage, PipeLineSession session) throws ListenerException;
}
