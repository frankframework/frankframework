/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2023, 2024 WeAreFrank!

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

import jakarta.annotation.Nonnull;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.doc.ElementType;
import org.frankframework.doc.ElementType.ElementTypes;
import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.stream.Message;

/**
 * Base-interface for IPullingListener and IPushingListener.
 * @param <M> the raw message type
 *
 * @author  Gerrit van Brakel
 * @since   4.2
 */
@FrankDocGroup(FrankDocGroupValue.LISTENER)
@ElementType(ElementTypes.LISTENER)
public interface IListener<M> extends IConfigurable {

	/**
	 * <code>configure()</code> is called once at startup of the framework in the <code>configure()</code> method
	 * of the owner of this listener.
	 * Purpose of this method is to reduce creating connections to databases etc. in the {@link IPullingListener#getRawMessage(Map)} method.
	 * As much as possible class-instantiating should take place in the
	 * <code>configure()</code> or <code>open()</code> method, to improve performance.
	 */
	@Override
	void configure() throws ConfigurationException;

	/**
	 * Prepares the listener for receiving messages.
	 * <code>open()</code> is called once each time the listener is started.
	 */
	void open() throws ListenerException;

	/**
	 * Close all resources used for listening.
	 * Called once each time the listener is stopped.
	 */
	void close() throws ListenerException;

	/**
	 * Extracts data from message obtained from {@link IPullingListener#getRawMessage(Map)} or
	 * {@link IPushingListener#wrapRawMessage(Object, PipeLineSession)}. May also extract
	 * other parameters from the message and put those into the context.
	 *
	 * @param rawMessage The {@link RawMessageWrapper} from which to extract the {@link Message}.
	 * @param context Context to populate. Either a {@link PipeLineSession} or a {@link Map} threadContext depending on caller.
	 * @return input {@link Message} for adapter.
	 *
	 */
	Message extractMessage(@Nonnull RawMessageWrapper<M> rawMessage, @Nonnull Map<String,Object> context) throws ListenerException;

	/**
	 * Called to perform actions (like committing or sending a reply) after a message has been processed by the
	 * Pipeline.
	 */
	void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<M> rawMessage, PipeLineSession pipeLineSession) throws ListenerException;

}
