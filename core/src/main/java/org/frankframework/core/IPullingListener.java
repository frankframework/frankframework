/*
   Copyright 2013 Nationale-Nederlanden, 2023 WeAreFrank!

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

import org.frankframework.receivers.RawMessageWrapper;

/**
 * Defines listening behaviour of pulling receivers.
 * Pulling receivers are receivers that poll for a message, as opposed to pushing receivers
 * that are 'message driven'
 * @param <M> the raw message type
 *
 * @author  Gerrit van Brakel
 */
public interface IPullingListener<M> extends IListener<M> {

	/**
	 * Prepares a thread for receiving messages.
	 * Called once for each thread that will listen for messages.
	 * @return the threadContext for this thread. The threadContext is a Map in which
	 * thread-specific data can be stored. May not be {@code null}, must be a mutable map type.
	 */
	@Nonnull
	Map<String,Object> openThread() throws ListenerException;

	/**
	 * Finalizes a message receiving thread.
	 * Called once for each thread that listens for messages, just before
	 * {@link #stop()} is called.
	 */
	void closeThread(@Nonnull Map<String,Object> threadContext) throws ListenerException;

	/**
	 * Retrieves messages from queue or other channel, but does no processing on it.
	 * Multiple objects may try to call this method at the same time, from different threads.
	 * Implementations of this method should therefore be thread-safe, or <code>synchronized</code>.
	 * <p>Any thread-specific properties should be stored in and retrieved from the threadContext.
	 */
	RawMessageWrapper<M> getRawMessage(@Nonnull Map<String,Object> threadContext) throws ListenerException;

}
