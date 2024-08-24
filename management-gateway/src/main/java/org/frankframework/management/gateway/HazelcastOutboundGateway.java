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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.RandomStringUtils;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.gateway.events.ClusterMemberEvent;
import org.frankframework.management.gateway.events.ClusterMemberEvent.EventType;
import org.frankframework.util.SpringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.core.GenericMessagingTemplate;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Log4j2
public class HazelcastOutboundGateway implements InitializingBean, ApplicationContextAware, OutboundGateway {
	private HazelcastInstance hzInstance;
	private ApplicationContext applicationContext;

	private final String requestTopicName = HazelcastConfig.REQUEST_TOPIC_NAME;
	private ITopic<Message<?>> requestTopic;

	@Override
	@Nonnull
	public <I, O> Message<O> sendSyncMessage(Message<I> in) {
		String tempReplyChannelName = "__tmp."+ RandomStringUtils.randomAlphanumeric(32);
		long receiveTimeout = receiveTimeout(in);
		log.debug("sending synchronous request to topic [{}] message [{}] reply-queue [{}] receiveTimeout [{}]", requestTopicName, in, tempReplyChannelName, receiveTimeout);

		// Create the response queue here, before sending the request.
		IQueue<Message<O>> responseQueue = hzInstance.getQueue(tempReplyChannelName);

		Message<I> requestMessage = MessageBuilder.fromMessage(in)
				.setReplyChannelName(tempReplyChannelName)
				.setHeader(HazelcastConfig.AUTHENTICATION_HEADER_KEY, getAuthentication())
				.build();
		requestTopic.publish(requestMessage);

		Message<O> replyMessage = doReceive(responseQueue, receiveTimeout);
		if (replyMessage != null) {
			return replyMessage;
		}

		throw new BusException("no reponse found on temporary reply-queue ["+tempReplyChannelName+"] within receiveTimeout ["+receiveTimeout+"]");
	}

	@Nullable
	private <O> Message<O> doReceive(IQueue<Message<O>> responseQueue, long receiveTimeout) {
		try {
			Message<O> response = responseQueue.poll(receiveTimeout, TimeUnit.MILLISECONDS);

			if(response != null) {
				log.trace("received message with id [{}]", () -> response.getHeaders().getId());
				return response;
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			responseQueue.destroy();
		}

		log.trace("did not receive response within timeout of [{}] ms", receiveTimeout);
		return null;
	}

	@Override
	public List<ClusterMember> getMembers() {
		Set<Member> members = hzInstance.getCluster().getMembers();
		return members.stream().map(this::mapMember).toList();
	}

	private ClusterMember mapMember(Member member) {
		ClusterMember cm = new ClusterMember();
		cm.setAddress(member.getSocketAddress().getHostName() + ":" + member.getSocketAddress().getPort());
		cm.setId(member.getUuid());
		Map<String, String> attrs = new HashMap<>(member.getAttributes());
		cm.setType(attrs.remove(HazelcastConfig.ATTRIBUTE_TYPE_KEY));
		cm.setAttributes(attrs);
		cm.setLocalMember(member.localMember());
		return cm;
	}

	private @Nonnull Authentication getAuthentication() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if(authentication == null) {
			throw new AuthenticationServiceException("no Authentication object found in SecurityContext"); //This should technically not be possible but...
		}
		return authentication;
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
	public <I> void sendAsyncMessage(Message<I> in) {
		log.debug("sending asynchronous request to topic [{}] message [{}]", requestTopicName, in);
		Message<I> requestMessage = MessageBuilder.fromMessage(in)
				.setReplyChannelName(null)
				.setHeader(HazelcastConfig.AUTHENTICATION_HEADER_KEY, getAuthentication())
				.build();

		requestTopic.publishAsync(requestMessage);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		hzInstance = HazelcastConfig.newHazelcastInstance("console");
		SpringUtils.registerSingleton(applicationContext, "hazelcastOutboundInstance", hzInstance);

		requestTopic = hzInstance.getTopic(requestTopicName);

		hzInstance.getCluster().addMembershipListener(new MembershipListener() {

			@Override
			public void memberAdded(MembershipEvent e) {
				applicationContext.publishEvent(new ClusterMemberEvent(applicationContext, EventType.ADD_MEMBER, mapMember(e.getMember())));
			}

			@Override
			public void memberRemoved(MembershipEvent e) {
				applicationContext.publishEvent(new ClusterMemberEvent(applicationContext, EventType.REMOVE_MEMBER, mapMember(e.getMember())));
			}

		});
	}
}
