/*
   Copyright 2024-2026 WeAreFrank!

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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.core.GenericMessagingTemplate;

import com.hazelcast.cluster.Member;
import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.LifecycleService;
import com.hazelcast.map.IMap;
import com.hazelcast.topic.ITopic;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;

import lombok.extern.log4j.Log4j2;

import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.gateway.HazelcastConfig.InstanceType;
import org.frankframework.management.security.AbstractJwtGenerator;
import org.frankframework.util.SpringUtils;

@Log4j2
public class HazelcastOutboundGateway implements ApplicationContextAware, OutboundGateway, SmartLifecycle, DisposableBean {
	private HazelcastInstance hzInstance;
	private LifecycleService lifecycle;
	private ApplicationContext applicationContext;

	private static final RandomStringUtils NUMBER_GENERATOR = RandomStringUtils.insecure();
	private final String requestTopicName;
	private ITopic<Message<?>> requestTopic;

	public HazelcastOutboundGateway() {
		this(HazelcastConfig.REQUEST_TOPIC_NAME);
	}

	// Testable
	protected HazelcastOutboundGateway(String requestTopicName) {
		this.requestTopicName = requestTopicName;
	}

	@Autowired
	private AbstractJwtGenerator<?> jwtGenerator;

	@Override
	public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void start() {
		hzInstance = HazelcastConfig.newHazelcastInstance(InstanceType.CONTROLLER, Collections.emptyMap());
		SpringUtils.registerSingleton(applicationContext, "hazelcastOutboundInstance", hzInstance);
		lifecycle = hzInstance.getLifecycleService();

		requestTopic = hzInstance.getTopic(requestTopicName);

		hzInstance.getCluster().addMembershipListener(new HazelcastMembershipListener(applicationContext));

		JWK jwk = jwtGenerator.getPublicJwk();
		if (jwk != null) {
			updateJwks(jwk);
		}
	}

	/**
	 * Update JWKS for new worker nodes.
	 */
	private void updateJwks(@NonNull JWK jwk) {
		IMap<String, String> config = hzInstance.getMap(HazelcastConfig.FRANK_APPLICATION_CONFIG);
		String jwks = config.get(HazelcastConfig.FRANK_APPLICATION_KEYSET);

		if (StringUtils.isBlank(jwks)) {
			config.set(HazelcastConfig.FRANK_APPLICATION_KEYSET, new JWKSet(jwk).toString());
		} else {
			List<JWK> existingJwks = getJwks(jwks);

			existingJwks.add(jwk);
			JWKSet updatedJwks = new JWKSet(existingJwks);
			config.set(HazelcastConfig.FRANK_APPLICATION_KEYSET, updatedJwks.toString());
		}
	}

	private static @NonNull List<JWK> getJwks(String jwks) {
		try {
			return new ArrayList<>(JWKSet.parse(jwks).getKeys());
		} catch (ParseException e) {
			log.error("unable to parse JWK set, creating a new one...", e);
			return new ArrayList<>();
		}
	}

	/**
	 * Remove old JWK when no longer needed.
	 */
	@Override
	public void stop() {
		JWK jwk = jwtGenerator.getPublicJwk();
		if (jwk != null) {
			IMap<String, String> config = hzInstance.getMap(HazelcastConfig.FRANK_APPLICATION_CONFIG);
			String jwks = config.get(HazelcastConfig.FRANK_APPLICATION_KEYSET);
			List<JWK> existingJwks = getJwks(jwks);
			existingJwks.remove(jwk);
			JWKSet updatedJwks = new JWKSet(existingJwks);
			config.set(HazelcastConfig.FRANK_APPLICATION_KEYSET, updatedJwks.toString());
		}

		lifecycle.shutdown();
	}

	@Override
	public boolean isRunning() {
		return lifecycle != null && lifecycle.isRunning();
	}

	@Override
	public void destroy() {
		lifecycle.terminate();
	}

	@Override
	@NonNull
	public <I, O> Message<O> sendSyncMessage(Message<I> in) {
		String tempReplyChannelName = "__tmp." + NUMBER_GENERATOR.nextAlphanumeric(32);
		long receiveTimeout = receiveTimeout(in);
		log.debug("sending synchronous request to topic [{}] message [{}] reply-queue [{}] receiveTimeout [{}]", requestTopicName, in, tempReplyChannelName, receiveTimeout);

		// Create the response queue here, before sending the request.
		IQueue<Message<O>> responseQueue = hzInstance.getQueue(tempReplyChannelName);

		Message<I> requestMessage = HazelcastMessageBuilder.fromMessage(in)
				.setReplyChannelName(tempReplyChannelName)
				.setAuthentication(getAuthentication(in))
				.build();
		requestTopic.publish(requestMessage);

		Message<O> replyMessage = doReceive(responseQueue, receiveTimeout);
		silentlyRemoveQueue(responseQueue);
		if (replyMessage != null) {
			return replyMessage;
		}

		throw new BusException("no response found on temporary reply-queue [" + tempReplyChannelName + "] within receiveTimeout [" + receiveTimeout + "]");
	}

	private void silentlyRemoveQueue(IQueue<?> responseQueue) {
		try {
			responseQueue.destroy();
		} catch (Exception e) {
			log.info("error closing response queue", e);
		}
	}

	@Nullable
	private <O> Message<O> doReceive(IQueue<Message<O>> responseQueue, long receiveTimeout) {
		try {
			Message<O> response = responseQueue.poll(receiveTimeout, TimeUnit.MILLISECONDS);

			if (response != null) {
				log.trace("received message with id [{}]", () -> response.getHeaders().getId());
				return response;
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		log.trace("did not receive response within timeout of [{}] ms", receiveTimeout);
		return null;
	}

	@NonNull
	@Override
	public List<ClusterMember> getMembers() {
		Set<Member> members = hzInstance.getCluster().getMembers();
		return members.stream().map(HazelcastMembershipListener::mapMember).toList();
	}

	private @NonNull String getAuthentication(Message<?> message) {
		return jwtGenerator.createJWT(builder -> {
			UUID target = message.getHeaders().get(BusMessageUtils.HEADER_TARGET_KEY, UUID.class);
			if (target != null) {
				builder.audience(target.toString());
			}
		});
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
		Message<I> requestMessage = HazelcastMessageBuilder.fromMessage(in)
				.setReplyChannelName(null)
				.setAuthentication(getAuthentication(in))
				.build();

		requestTopic.publishAsync(requestMessage);
	}
}
