/*
   Copyright 2024-2025 WeAreFrank!

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
package org.frankframework.console.util;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.util.JacksonUtils;

@Log4j2
public class RequestMessageBuilder {
	private final Map<String, Object> customHeaders = new HashMap<>();

	private final @Getter @Nonnull BusTopic topic;
	private final @Getter @Nonnull BusAction action;

	private static final String DEFAULT_PAYLOAD = "NONE";
	private Object payload = DEFAULT_PAYLOAD;

	private static final Logger SEC_LOG = LogManager.getLogger("SEC");

	public RequestMessageBuilder(@Nonnull BusTopic topic) {
		this(topic, BusAction.GET);
	}

	public RequestMessageBuilder(@Nonnull BusTopic topic, @Nonnull BusAction action) {
		this.topic = topic;
		this.action = action;
	}

	public RequestMessageBuilder addHeader(String key, String value) {
		addCustomHeader(key, value);
		return this;
	}

	public RequestMessageBuilder addHeader(String key, Integer value) {
		addCustomHeader(key, value);
		return this;
	}

	public RequestMessageBuilder addHeader(String key, Boolean value) {
		addCustomHeader(key, value);
		return this;
	}

	private void addCustomHeader(String key, Object value) {
		if (BusTopic.TOPIC_HEADER_NAME.equals(key)) {
			throw new IllegalStateException("unable to override topic header");
		}
		customHeaders.put(key, value);
	}

	public RequestMessageBuilder setPayload(InputStream payload) {
		this.payload = payload;
		return this;
	}

	public RequestMessageBuilder setJsonPayload(Object payload) {
		return setPayload(JacksonUtils.convertToJson(payload));
	}

	public RequestMessageBuilder setPayload(String payload) {
		this.payload = payload;
		return this;
	}

	public static RequestMessageBuilder create(@Nonnull BusTopic topic) {
		return new RequestMessageBuilder(topic);
	}

	public static RequestMessageBuilder create(@Nonnull BusTopic topic, @Nonnull BusAction action) {
		return new RequestMessageBuilder(topic, action);
	}

	/**
	 * Log relevant information to the Security-log.
	 * GET requests, and requests without payload, are logged on debug level.
	 * 
	 * When a GET request contains a payload we're doing something wrong...
	 * Ideally only the `upload` TOPIC should use a payload.
	 */
	private void addLogLines() {
		String headers = customHeaders.entrySet().stream()
				.map(this::mapHeaderForLog)
				.collect(Collectors.joining(", "));

		if (action == BusAction.GET || DEFAULT_PAYLOAD.equals(payload)) {
			if(action == BusAction.GET && !DEFAULT_PAYLOAD.equals(payload)) {
				log.warn("created bus request [GET:{}] with payload [{}]", topic, payload);
			}

			SEC_LOG.debug("created bus request [{}:{}] with headers [{}]", action, topic, headers);
		} else {
			String safePayload = payload instanceof String payloadString ? StringEscapeUtils.escapeJava(payloadString) : "";
			SEC_LOG.info("created bus request [{}:{}] with headers [{}] payload [{}]", action, topic, headers, safePayload);
		}
	}

	public Message<?> build(@Nullable UUID uuid) {
		if (SEC_LOG.isInfoEnabled()) {
			addLogLines();
		}

		MessageBuilder<?> builder = MessageBuilder.withPayload(payload);
		builder.setHeader(BusTopic.TOPIC_HEADER_NAME, topic.name());
		builder.setHeader(BusAction.ACTION_HEADER_NAME, action.name());

		// Optional target parameter, to target a specific backend node.
		if(uuid != null) {
			builder.setHeader(BusMessageUtils.HEADER_TARGET_KEY, uuid);
		}

		for (Map.Entry<String, Object> customHeader : customHeaders.entrySet()) {
			String key = BusMessageUtils.HEADER_PREFIX + customHeader.getKey();
			builder.setHeader(key, customHeader.getValue());
		}

		return builder.build();
	}

	private String mapHeaderForLog(Map.Entry<String, Object> entry) {
		StringBuilder builder = new StringBuilder(entry.getKey());
		builder.append("=");

		Object value = entry.getValue();
		builder.append(value instanceof String s ? sanitizeForLog(s) : value);

		return builder.toString();
	}

	private String sanitizeForLog(String value) {
		return value.replace("\b\n\t\f\r", "");
	}
}
