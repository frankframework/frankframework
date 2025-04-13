/*
   Copyright 2021-2024 WeAreFrank!

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

import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.Logger;

import org.frankframework.core.IListener;
import org.frankframework.core.IMessageHandler;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.TimeoutException;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.stream.Message;
import org.frankframework.util.LogUtil;

/**
 * Message handler for JavaListener and WebServiceListener.
 *
 * @author Jaco de Groot
 * @author Niels Meijer
 */
public class ListenerMessageHandler<M> implements IMessageHandler<M> {
	private static final Logger log = LogUtil.getLogger(ListenerMessageHandler.class);
	private final BlockingQueue<ListenerMessage> requestMessages = new ArrayBlockingQueue<>(100);
	private final BlockingQueue<ListenerMessage> responseMessages = new ArrayBlockingQueue<>(100);

	private long defaultTimeout = LarvaTool.globalTimeoutMillis;

	@Override
	public Message processRequest(IListener<M> origin, RawMessageWrapper<M> rawMessage, Message message, PipeLineSession session) throws ListenerException {
		try {
			ListenerMessage requestMessage = new ListenerMessage(message, session);
			requestMessages.add(requestMessage);

			ListenerMessage responseMessage = getResponseMessage(defaultTimeout);
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
	public ListenerMessage getRequestMessage() {
		try {
			return getRequestMessage(0);
		} catch (TimeoutException e) {
			return null;
		}
	}

	public ListenerMessage getRequestMessageWithDefaultTimeout() throws TimeoutException {
		return getRequestMessage(defaultTimeout);
	}

	/** Attempt to retrieve a {@link ListenerMessage} with timeout in ms. Returns TimeOutException if non is present */
	private ListenerMessage getRequestMessage(long timeout) throws TimeoutException {
		if (timeout <= 0) {
			timeout = defaultTimeout;
		}
		return getMessageFromQueue(requestMessages, timeout, "request");
	}

	private ListenerMessage getMessageFromQueue(BlockingQueue<ListenerMessage> queue, long timeout, String messageType) throws TimeoutException {
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
	public ListenerMessage getResponseMessage() {
		try {
			return getResponseMessage(0);
		} catch (TimeoutException e) {
			return null;
		}
	}

	/** Attempt to retrieve a {@link ListenerMessage} with timeout in ms. Returns TimeOutException if non is present */
	private ListenerMessage getResponseMessage(long timeout) throws TimeoutException {
		return getMessageFromQueue(responseMessages, timeout, "response");
	}

	public void putResponseMessage(ListenerMessage listenerMessage) {
		if (listenerMessage != null) {
			responseMessages.add(listenerMessage);
		} else {
			log.error("listenerMessage is null");
		}
	}

	public void setTimeout(long defaultTimeout) {
		this.defaultTimeout = defaultTimeout;
	}

	public void setRequestTimeOut(int timeout) {
		setTimeout(timeout);
	}

	public void setResponseTimeOut(int timeout) {
		setTimeout(timeout);
	}

	@Override
	public void processRawMessage(IListener<M> origin, RawMessageWrapper<M> rawMessage, PipeLineSession threadContext, boolean duplicatesAlreadyChecked) throws ListenerException {
		Message message = origin.extractMessage(rawMessage, threadContext);
		processRequest(origin, rawMessage, message, threadContext);
	}


	@Override
	public Message formatException(String extraInfo, String arg1, Message arg2, Throwable arg3) {
		NotImplementedException e = new NotImplementedException();
		log.error("formatException not implemented", e);
		return null;
	}
}
