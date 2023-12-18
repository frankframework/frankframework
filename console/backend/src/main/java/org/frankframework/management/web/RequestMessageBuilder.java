/*
   Copyright 2022-2023 WeAreFrank!

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
package org.frankframework.management.web;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import lombok.Getter;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;

import org.frankframework.util.HttpUtils;

import org.frankframework.util.JacksonUtils;

public class RequestMessageBuilder {
	private Map<String, Object> customHeaders = new HashMap<>();

	private final FrankApiBase base;
	private final @Getter BusTopic topic;
	private final @Getter BusAction action;
	private Object payload = "NONE";

	private static final Logger SEC_LOG = LogManager.getLogger("SEC");

	public RequestMessageBuilder(FrankApiBase base, BusTopic topic) {
		this(base, topic, null);
	}

	public RequestMessageBuilder(FrankApiBase base, BusTopic topic, BusAction action) {
		this.base = base;
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
		if(BusTopic.TOPIC_HEADER_NAME.equals(key)) {
			throw new IllegalStateException("unable to override topic header");
		}
		customHeaders.put(key, value);
	}

	public RequestMessageBuilder setJsonPayload(Object payload) {
		this.payload = JacksonUtils.convertToJson(payload);
		return this;
	}

	public RequestMessageBuilder setPayload(InputStream payload) {
		this.payload = payload;
		return this;
	}

	public RequestMessageBuilder setPayload(String payload) {
		this.payload = payload;
		return this;
	}

	public static RequestMessageBuilder create(FrankApiBase base, BusTopic topic) {
		return new RequestMessageBuilder(base, topic);
	}

	public static RequestMessageBuilder create(FrankApiBase base, BusTopic topic, BusAction action) {
		return new RequestMessageBuilder(base, topic, action);
	}

	public Message<?> build() {
		if(SEC_LOG.isInfoEnabled()) {
			String method = base.getServletRequest().getMethod();
			String issuedBy = sanitizeForLog(HttpUtils.getCommandIssuedBy(base.getServletRequest()));
			if((method.equalsIgnoreCase("GET") || method.equalsIgnoreCase("OPTIONS"))) {
				SEC_LOG.debug("created bus request from URI [{}:{}] issued by{}", method, base.getUriInfo().getRequestUri(), issuedBy);
			} else {
				String headers = customHeaders.entrySet().stream()
						.map(this::mapHeaderForLog)
						.collect(Collectors.joining (", "));
				SEC_LOG.info("created bus request from URI [{}:{}] issued by{} with headers [{}] payload [{}]", method, base.getUriInfo().getRequestUri(), issuedBy, headers, payload);
			}
		}

		DefaultMessageBuilderFactory factory = base.getApplicationContext().getBean("messageBuilderFactory", DefaultMessageBuilderFactory.class);
		MessageBuilder<?> builder = factory.withPayload(payload);
		builder.setHeader(BusTopic.TOPIC_HEADER_NAME, topic.name());
		if(action != null) {
			builder.setHeader(BusAction.ACTION_HEADER_NAME, action.name());
		}


		for(Entry<String, Object> customHeader : customHeaders.entrySet()) {
			String key = BusMessageUtils.HEADER_PREFIX + customHeader.getKey();
			builder.setHeader(key, customHeader.getValue());
		}

		return builder.build();
	}

	private String mapHeaderForLog(Entry<String, Object> entry) {
		StringBuilder builder = new StringBuilder(entry.getKey());
		builder.append("=");

		Object value = entry.getValue();
		builder.append((value instanceof String) ? sanitizeForLog((String) value) : value);

		return builder.toString();
	}

	private String sanitizeForLog(String value) {
		return value.replace("\b\n\t\f\r", "");
	}
}
