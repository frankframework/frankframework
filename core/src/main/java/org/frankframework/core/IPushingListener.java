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

import java.util.Map;

import org.jspecify.annotations.NonNull;

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
	void setHandler(@NonNull IMessageHandler<M> handler);

	/**
	 * Set a (single) listener that will be notified of any exceptions.
	 * The listener should use this listener to notify the receiver of
	 * any exception that occurs outside the processing of a message.
	 */
	void setExceptionListener(@NonNull IbisExceptionListener listener);

	/**
	 * Wrap a raw message in a MessageWrapper. Populate {@link PipeLineSession} with properties
	 * from the message.
	 * <p>If the returned object is an instance of {@link org.frankframework.receivers.MessageWrapper} then the {@link org.frankframework.receivers.Receiver#processRawMessage(IListener, RawMessageWrapper, PipeLineSession, boolean)}
	 * will not call {@link IListener#extractMessage(RawMessageWrapper, Map)} but directly access the enclosed {@link org.frankframework.stream.Message} and the
	 * {@link RawMessageWrapper#getContext()}</p>.
	 *
	 * @param rawMessage The raw message data, unwrapped
	 * @param session {@link PipeLineSession} to populate with properties from the message.
	 * @return Wrapped raw message
	 * @throws ListenerException If any exception occurs during wrapping, a {@link ListenerException} is thrown.
	 */
	RawMessageWrapper<@NonNull M> wrapRawMessage(@NonNull M rawMessage, @NonNull PipeLineSession session) throws ListenerException;
}
