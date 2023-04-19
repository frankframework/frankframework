/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.management.web;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.UriInfo;

import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import lombok.Getter;
import nl.nn.adapterframework.http.HttpUtils;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusTopic;

public class RequestMessageBuilder {
	private Map<String, Object> customHeaders = new HashMap<>();

	private final FrankApiBase base;
	private final @Getter BusTopic topic;
	private final @Getter BusAction action;
	private Object payload = "NONE";

	public RequestMessageBuilder(FrankApiBase base, BusTopic topic) {
		this(base, topic, null);
	}

	public RequestMessageBuilder(FrankApiBase base, BusTopic topic, BusAction action) {
		this.base = base;
		this.topic = topic;
		this.action = action;
	}

	public RequestMessageBuilder addHeader(String key, Object value) {
		if(BusTopic.TOPIC_HEADER_NAME.equals(key)) {
			throw new IllegalStateException("unable to override topic header");
		}
		customHeaders.put(key, value);
		return this;
	}

	public RequestMessageBuilder setPayload(Object payload) {
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
		DefaultMessageBuilderFactory factory = base.getApplicationContext().getBean("messageBuilderFactory", DefaultMessageBuilderFactory.class);
		MessageBuilder<?> builder = factory.withPayload(payload);
		builder.setHeader(BusTopic.TOPIC_HEADER_NAME, topic.name());
		if(action != null) {
			builder.setHeader(BusAction.ACTION_HEADER_NAME, action.name());
		}

		UriInfo uriInfo = base.getUriInfo();
		URI uri = uriInfo.getRequestUri();
		if(uri != null) {
			builder.setHeader("request-uri", uri.toString());
		}
		builder.setHeader("request-method", base.getServletRequest().getMethod());

		if(uriInfo.getQueryParameters() != null && !uriInfo.getQueryParameters().isEmpty()) {
			builder.setHeader("request-query", uriInfo.getQueryParameters());
		}
		if(uriInfo.getPathParameters() != null && !uriInfo.getPathParameters().isEmpty()) {
			builder.setHeader("request-path", uriInfo.getPathParameters());
		}

		builder.setHeader("issuedBy", HttpUtils.getExtendedCommandIssuedBy(base.getServletRequest()));

		for(Entry<String, Object> customHeader : customHeaders.entrySet()) {
			builder.setHeader(customHeader.getKey(), customHeader.getValue());
		}

		return builder.build();
	}
}
