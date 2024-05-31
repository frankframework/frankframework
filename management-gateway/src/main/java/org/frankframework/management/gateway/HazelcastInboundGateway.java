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

import java.io.InputStream;
import java.util.UUID;

import org.apache.logging.log4j.CloseableThreadContext;
import org.frankframework.util.SpringUtils;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.impl.DefaultNodeContext;
import com.hazelcast.instance.impl.HazelcastInstanceFactory;
import com.hazelcast.topic.ITopic;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class HazelcastInboundGateway extends MessagingGatewaySupport {
	private HazelcastInstance hzInstance;

	private String requestTopicName = HazelcastConfig.REQUEST_TOPIC_NAME;
	private ITopic<Message<?>> requestTopic;

	@Override
	protected void onInit() {
		hzInstance = HazelcastInstanceFactory.newHazelcastInstance(HazelcastConfig.createHazelcastConfig(), "worker-node", new DefaultNodeContext());
		SpringUtils.registerSingleton(getApplicationContext(), "hazelcastInboundInstance", hzInstance);
		requestTopic = hzInstance.getTopic(requestTopicName);

		if(getRequestChannel() == null) {
			MessageChannel requestChannel = getApplicationContext().getBean("frank-management-bus", MessageChannel.class);
			setRequestChannel(requestChannel);
		}

		super.onInit();

		UUID listenerId = requestTopic.addMessageListener(this::handleIncomingMessage);
		log.debug("created message listener [{}] on topic [{}]", listenerId, requestTopicName);
	}

	private <E extends Message<?>> void handleIncomingMessage(com.hazelcast.topic.Message<E> rawMessage) {
		E message = rawMessage.getMessageObject();
		UUID messageId = message.getHeaders().getId();
		log.trace("received message with id [{}] from member [{}]", () -> messageId, ()->rawMessage.getPublishingMember().getUuid());

		try (final CloseableThreadContext.Instance ctc = CloseableThreadContext.put("messageId", messageId.toString())) {
			String tempReplyChannel = (String) message.getHeaders().getReplyChannel();

			log.debug("received message [{}] {} reply-channel", message, tempReplyChannel == null ? "without" : "with");
			processMessage(message, tempReplyChannel);
		}
	}

	private void processMessage(Message<?> incomingMessage, String tempReplyChannel) {
		if(tempReplyChannel == null) { // send async
			log.trace("processing message asynchronous");
			super.send(incomingMessage);
		} else {
			log.trace("processing message synchronous");
			Message<?> response = super.sendAndReceiveMessage(incomingMessage);
			if(response != null) {
				UUID responseId = response.getHeaders().getId();
				if(response.getPayload() instanceof InputStream inputStream) {
					response = MessageBuilder.withPayload(new SerializableInputStream(inputStream)).copyHeaders(response.getHeaders()).build();
				}

				log.trace("sending response message with id [{}] to reply-channel [{}]", responseId, tempReplyChannel);
				IQueue<Message<?>> responseQueue = hzInstance.getQueue(tempReplyChannel);
				if(!responseQueue.offer(response)) {
					log.error("unable to send response [{}] to reply-channel [{}]", response,tempReplyChannel);
				}
			} else {
				log.trace("synchronous message did not return a response");
			}
		}
	}
}
