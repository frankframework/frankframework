/*
   Copyright 2013 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
import org.frankframework.stream.Message;

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
	 * Will use listener to perform {@link IListener#extractMessage} and {@link IListener#afterMessageProcessed}
	 * TODO Shouldn't this be a IPullingListener?
	 * TOOD Move {@link IListener#extractMessage} and {@link IListener#afterMessageProcessed} to {@link IPullingListener}.
	 */
	void processRawMessage(IListener<M> origin, RawMessageWrapper<M> message, PipeLineSession session, boolean duplicatesAlreadyChecked) throws ListenerException;

	/**
	 * Alternative to functions above, will NOT use {@link IListener#extractMessage}.
	 * TODO Remove pointless `RawMessage` wrapping and unwrapping.
	 */
	Message processRequest(IPushingListener<M> origin, RawMessageWrapper<M> rawMessage, Message message, PipeLineSession session) throws ListenerException;

}
