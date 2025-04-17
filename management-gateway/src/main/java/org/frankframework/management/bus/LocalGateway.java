/*
   Copyright 2022-2024 WeAreFrank!

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
package org.frankframework.management.bus;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.GenericMessagingTemplate;

/**
 * A Spring Integration Gateway in it's most simplistic form.
 * Put's messages on their respective Channels.
 */
public class LocalGateway extends MessagingGatewaySupport implements OutboundGateway {

	private static final long DEFAULT_REQUEST_TIMEOUT = 1000L;

	@Override
	protected void onInit() {
		if(getRequestChannel() == null) {
			MessageChannel requestChannel = getApplicationContext().getBean("frank-management-bus", MessageChannel.class);
			setRequestChannel(requestChannel);
		}

		setRequestTimeout(DEFAULT_REQUEST_TIMEOUT);
		setReplyTimeout(DEFAULT_REQUEST_TIMEOUT);

		super.onInit();
	}

	@Override
	@SuppressWarnings("unchecked")
	@Nonnull
	public <I, O> Message<O> sendSyncMessage(Message<I> requestMessage) {
		Message<O> replyMessage = (Message<O>) super.sendAndReceiveMessage(requestMessage);
		if (replyMessage != null) {
			return replyMessage;
		}

		long timeout = getSendTimeout(requestMessage) + getReceiveTimeout(requestMessage);
		throw new BusException("no reponse found on reply-queue within receiveTimeout ["+timeout+"]");
	}

	private long getSendTimeout(Message<?> requestMessage) {
		Long sendTimeout = convertHeaderToLong(requestMessage.getHeaders().get(GenericMessagingTemplate.DEFAULT_SEND_TIMEOUT_HEADER));
		return (sendTimeout != null ? sendTimeout : DEFAULT_REQUEST_TIMEOUT);
	}

	private long getReceiveTimeout(Message<?> requestMessage) {
		Long sendTimeout = convertHeaderToLong(requestMessage.getHeaders().get(GenericMessagingTemplate.DEFAULT_RECEIVE_TIMEOUT_HEADER));
		return (sendTimeout != null ? sendTimeout : DEFAULT_REQUEST_TIMEOUT);
	}

	@Nullable
	private Long convertHeaderToLong(@Nullable Object headerValue) {
		if (headerValue instanceof Number) {
			return ((Number) headerValue).longValue();
		}
		else if (headerValue instanceof String) {
			return Long.parseLong((String) headerValue);
		}
		else {
			return null;
		}
	}

	@Override
	public <I >void sendAsyncMessage(Message<I> in) {
		super.send(in);
	}

	/* must (re-)throw exceptions and not publish them to a dead-letter-queue. */
	@Override
	public MessageChannel getErrorChannel() {
		return null;
	}
}
