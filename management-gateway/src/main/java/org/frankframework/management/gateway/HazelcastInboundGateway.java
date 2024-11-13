/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.management.gateway;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.logging.log4j.CloseableThreadContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.MessageListener;

import lombok.Lombok;
import lombok.extern.log4j.Log4j2;

import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.security.JwtVerifier;
import org.frankframework.util.SpringUtils;

@Log4j2
public class HazelcastInboundGateway extends MessagingGatewaySupport {

	private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

	private HazelcastInstance hzInstance;

	private final String requestTopicName = HazelcastConfig.REQUEST_TOPIC_NAME;
	private ITopic<Message<?>> requestTopic;
	private JwtVerifier jwtVerifier;

	@Value("${instance.name:}")
	private String instanceName;

	@Override
	protected void onInit() {
		Map<String, String> attributes = Map.of(HazelcastConfig.ATTRIBUTE_APPLICATION_KEY, instanceName);
		hzInstance = HazelcastConfig.newHazelcastInstance("worker", attributes);
		SpringUtils.registerSingleton(getApplicationContext(), "hazelcastInboundInstance", hzInstance);
		requestTopic = hzInstance.getTopic(requestTopicName);

		IMap<String, String> config = hzInstance.getMap("frank-configuration");
		jwtVerifier = new JwtVerifier(() -> config.get("jwks"));

		setRequestChannel(getRequestChannel(getApplicationContext()));
		setErrorChannel(null); // no ErrorChannel means throw the exception, we catch it later in #processMessage(Message, String)

		super.onInit();

		UUID listenerId = requestTopic.addMessageListener(this::handleIncomingMessage);
		log.debug("created message listener [{}] on topic [{}]", listenerId, requestTopicName);
	}

	private MessageChannel getRequestChannel(ApplicationContext applicationContext) {
		return applicationContext.getBean("frank-management-bus", MessageChannel.class);
	}

	/**
	 * The Hazelcast {@link MessageListener} that handles all incoming Hazelcast traffic on topic {@link HazelcastConfig#REQUEST_TOPIC_NAME}.
	 * @param <E> Spring Integration {@link Message}
	 * @param rawMessage Hazelcast 'raw' object
	 */
	private <E extends Message<?>> void handleIncomingMessage(com.hazelcast.topic.Message<E> rawMessage) {
		E message = rawMessage.getMessageObject();

		UUID instanceId = hzInstance.getLocalEndpoint().getUuid();
		UUID filterId = message.getHeaders().get(BusMessageUtils.HEADER_TARGET_KEY, UUID.class);
		UUID messageId = message.getHeaders().getId();
		if (filterId != null && !filterId.equals(instanceId)) {
			log.trace("skipping message with id [{}] from member [{}]", () -> messageId, () -> rawMessage.getPublishingMember().getUuid());
			return;
		}

		log.trace("received message with id [{}] from member [{}]", () -> messageId, () -> rawMessage.getPublishingMember().getUuid());

		try (final CloseableThreadContext.Instance ctc = CloseableThreadContext.put("messageId", messageId.toString())) {
			String tempReplyChannel = (String) message.getHeaders().getReplyChannel();

			log.debug("received message [{}] {} reply-channel", message, tempReplyChannel == null ? "without" : "with");
			processMessage(message, tempReplyChannel);
		}
	}

	/**
	 * Process the message asynchronous or synchronous and handle all Exceptions.
	 * @param incomingMessage The Spring Integration message to send the RequestChannel
	 * @param tempReplyChannel The optional Hazelcast response queue name
	 */
	private void processMessage(@Nonnull Message<?> incomingMessage, @Nullable String tempReplyChannel) {
		MessageHeaders headers = incomingMessage.getHeaders();
		try {
			propagateAuthenticationContext(headers);

			if (tempReplyChannel == null) { // send async
				log.trace("processing message id [{}] asynchronous", headers::getId);
				super.send(incomingMessage);
			} else {
				log.trace("processing message id [{}] synchronous", headers::getId);
				Message<?> response = super.sendAndReceiveMessage(incomingMessage);
				if (response == null) {
					log.trace("synchronous message did not return a response");
					return;
				}
				handleResponse(response, tempReplyChannel);
			}
		} catch (MessageHandlingException e) {
			log.warn("error processing message id [{}]", headers.getId(), e.getCause());
		} catch (Exception e) {
			log.error("error processing message id [{}]", headers.getId(), e);
		}
	}

	/**
	 * Handles the response message. If the response is an {@link ErrorMessage} throw it immediately.
	 * @param response the (serializable) response message, may be an {@link ErrorMessage}.
	 * @param tempReplyChannel the {@link IQueue Hazelcast queue} name.
	 */
	private void handleResponse(Message<?> response, @Nonnull final String tempReplyChannel) {
		MessageHeaders headers = response.getHeaders();

		if (response instanceof ErrorMessage errMsg) {
			throw Lombok.sneakyThrow(errMsg.getPayload());
		}
		if (response.getPayload() instanceof InputStream inputStream) {
			response = MessageBuilder.withPayload(new SerializableInputStream(inputStream)).copyHeaders(headers).build();
		}

		log.trace("sending response message id [{}] to reply-channel [{}]", headers::getId, () -> tempReplyChannel);
		IQueue<Message<?>> responseQueue = hzInstance.getQueue(tempReplyChannel);
		if (!responseQueue.offer(response)) {
			log.error("unable to send response [{}] to reply-channel [{}]", response, tempReplyChannel);
		}
	}

	/**5
	 * Fetch the {@link Authentication} object out of the {@link HazelcastConfig#AUTHENTICATION_HEADER_KEY} header.
	 * Register the auth object in the current {@link SecurityContext} so Spring-Security can apply JSR250 authentication.
	 * @param headers Request MessageHeaders which should contain the {@link Authentication} object.
	 */
	private void propagateAuthenticationContext(@Nonnull MessageHeaders headers) throws IOException {
		Object auth = headers.get(HazelcastConfig.AUTHENTICATION_HEADER_KEY);
		Authentication authentication = createAuthenticationToken(auth);

		SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
		context.setAuthentication(authentication);
		this.securityContextHolderStrategy.setContext(context);
	}

	private Authentication createAuthenticationToken(Object authenticationObject) throws IOException {
		if(authenticationObject instanceof Authentication authentication) {
			return authentication;
		} else if(authenticationObject instanceof String jwt) {
			return jwtVerifier.verify(jwt);
		}

		throw new IOException("no authentication object found");
	}
}
