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

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.util.SpringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.core.GenericMessagingTemplate;

import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.impl.DefaultNodeContext;
import com.hazelcast.instance.impl.HazelcastInstanceFactory;
import com.hazelcast.topic.ITopic;

import jakarta.annotation.Nullable;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class HazelcastOutboundGateway<T> implements InitializingBean, ApplicationContextAware, OutboundGateway<T> {
	private HazelcastInstance hzInstance;
	private ApplicationContext applicationContext;

	private String requestTopicName = HazelcastConfig.REQUEST_TOPIC_NAME;
	private ITopic<Message<T>> requestTopic;

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.outbound_gateway;
	}

	@Override
	public Message<T> sendSyncMessage(Message<T> in) {
		String tempReplyChannelName = "__tmp."+ RandomStringUtils.randomAlphanumeric(32);
		long receiveTimeout = receiveTimeout(in);
		log.debug("sending synchronous request to topic [{}] message [{}] reply-queue [{}] receiveTimeout [{}]", requestTopicName, in, tempReplyChannelName, receiveTimeout);

		// Create the response queue here, before sending the request.
		IQueue<Message<T>> responseQueue = hzInstance.getQueue(tempReplyChannelName);

		Message<T> requestMessage = MessageBuilder.fromMessage(in).setReplyChannelName(tempReplyChannelName).build();
		requestTopic.publish(requestMessage);

		Message<T> replyMessage = doReceive(responseQueue, receiveTimeout);
		if (replyMessage != null) {
			return replyMessage;
		}

		throw new BusException("no reponse found on temporary reply-queue ["+tempReplyChannelName+"] within receiveTimeout ["+receiveTimeout+"]");
	}

	private @Nullable Message<T> doReceive(IQueue<Message<T>> responseQueue, long receiveTimeout) {
		try {
			Message<T> response = responseQueue.poll(receiveTimeout, TimeUnit.MILLISECONDS);

			log.trace("received message with id [{}]", () -> response.getHeaders().getId());
			return response;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			responseQueue.destroy();
		}
		log.trace("did not receive response within timeout of [{}] ms", receiveTimeout);

		return null;
	}

	private long receiveTimeout(Message<?> requestMessage) {
		Long receiveTimeout = headerToLong(requestMessage.getHeaders().get(GenericMessagingTemplate.DEFAULT_RECEIVE_TIMEOUT_HEADER));
		return (receiveTimeout != null ? receiveTimeout : 5000);
	}

	private @Nullable Long headerToLong(@Nullable Object headerValue) {
		if (headerValue instanceof Number number) {
			return number.longValue();
		} else if (headerValue instanceof String text) {
			return Long.parseLong(text);
		}
		return null;
	}

	@Override
	public void sendAsyncMessage(Message<T> in) {
		log.debug("sending asynchronous request to topic [{}] message [{}]", requestTopicName, in);
		Message<T> requestMessage = MessageBuilder.fromMessage(in).setReplyChannelName(null).build();

		requestTopic.publishAsync(requestMessage);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		hzInstance = HazelcastInstanceFactory.newHazelcastInstance(HazelcastConfig.createHazelcastConfig(), "console-node", new DefaultNodeContext());
		SpringUtils.registerSingleton(applicationContext, "hazelcastInstance", hzInstance);

		requestTopic = hzInstance.getTopic(requestTopicName);
	}
}
