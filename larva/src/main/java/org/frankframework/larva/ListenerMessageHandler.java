/*
   Copyright 2021-2025 WeAreFrank!

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
package org.frankframework.larva;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.IListener;
import org.frankframework.core.IMessageHandler;
import org.frankframework.core.IPushingListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.TimeoutException;
import org.frankframework.receivers.MessageWrapper;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.stream.Message;

/**
 * Message handler for JavaListener and WebServiceListener.
 * Only used for PushingListeners.
 * 
 * @author Niels Meijer
 */
@Log4j2
public class ListenerMessageHandler<M> implements IMessageHandler<M> {
	private final BlockingQueue<ListenerMessage> requestMessages = new ArrayBlockingQueue<>(10);
	private final BlockingQueue<ListenerMessage> responseMessages = new ArrayBlockingQueue<>(10);

	private long timeout;

	public ListenerMessageHandler(long defaultTimeout) {
		this.timeout = defaultTimeout;
	}

	@Override
	public Message processRequest(IPushingListener<M> origin, MessageWrapper<M> rawMessage, PipeLineSession session) throws ListenerException {
		return processRequest(rawMessage.getMessage(), session);
	}

	@Override
	public void processRawMessage(IListener<M> origin, RawMessageWrapper<M> rawMessage, PipeLineSession threadContext, boolean duplicatesAlreadyChecked) throws ListenerException {
		Message message = origin.extractMessage(rawMessage, threadContext);
		processRequest(message, threadContext);
	}

	private Message processRequest(Message message, PipeLineSession session) throws ListenerException {
		try {
			ListenerMessage requestMessage = new ListenerMessage(message, session);
			requestMessages.add(requestMessage);

			ListenerMessage responseMessage = getResponseMessage(timeout);
			Message responseAsMessage = responseMessage.getMessage();
			if (responseMessage.getContext() != null && responseMessage.getContext() != session) {
				// Sometimes the response has a different PipeLineSession than the original request. If we don't close it here, we'll leak it.
				responseMessage.getContext().close();
			}
			return responseAsMessage;
		} catch (TimeoutException e) {
			throw new ListenerException("error processing request", e);
		}
	}

	/** Attempt to retrieve a {@link ListenerMessage}. Returns NULL if none is present */
	public @Nullable ListenerMessage getRequestMessageOrNull() {
		return requestMessages.poll();
	}

	public @Nonnull ListenerMessage getRequestMessageWithDefaultTimeout() throws TimeoutException {
		return getRequestMessage(timeout);
	}

	/** Attempt to retrieve a {@link ListenerMessage} with timeout in ms. Returns TimeOutException if non is present */
	private @Nonnull ListenerMessage getRequestMessage(long timeout) throws TimeoutException {
		return getMessageFromQueue(requestMessages, timeout, "request");
	}

	private @Nonnull ListenerMessage getMessageFromQueue(BlockingQueue<ListenerMessage> queue, long timeout, String messageType) throws TimeoutException {
		try {
			ListenerMessage requestMessage = queue.poll(timeout, TimeUnit.MILLISECONDS);
			if(requestMessage != null) {
				return requestMessage;
			}
		} catch (InterruptedException e) {
			log.error("interrupted while waiting for {} message", messageType, e);
			Thread.currentThread().interrupt();
		}

		throw new TimeoutException();
	}

	/** Attempt to retrieve a {@link ListenerMessage}. Returns NULL if none is present */
	public @Nullable ListenerMessage getResponseMessageOrNull() {
		return responseMessages.poll();
	}

	/** Attempt to retrieve a {@link ListenerMessage} with timeout in ms. Returns TimeOutException if non is present */
	private @Nonnull ListenerMessage getResponseMessage(long timeout) throws TimeoutException {
		return getMessageFromQueue(responseMessages, timeout, "response");
	}

	public void putResponseMessage(ListenerMessage listenerMessage) {
		if (listenerMessage != null) {
			responseMessages.add(listenerMessage);
		} else {
			log.error("listenerMessage is null");
		}
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public void setRequestTimeOut(int timeout) {
		setTimeout(timeout);
	}

	public void setResponseTimeOut(int timeout) {
		setTimeout(timeout);
	}
}
