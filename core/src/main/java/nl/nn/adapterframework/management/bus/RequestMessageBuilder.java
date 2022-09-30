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
package nl.nn.adapterframework.management.bus;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.webcontrol.api.FrankApiBase;

public class RequestMessageBuilder {
	private Map<String, Object> customHeaders = new HashMap<>();

	private final FrankApiBase base;
	private final BusTopic topic;
	private final BusAction action;
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
		customHeaders.put(key, value);
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
		builder.setHeader(TopicSelector.TOPIC_HEADER_NAME, topic.name());
		if(action != null) {
			builder.setHeader(ActionSelector.ACTION_HEADER_NAME, action.name());
		}

		UriInfo uriInfo = base.getUriInfo();
		builder.setHeader("uri", uriInfo.getRequestUri());
		builder.setHeader("method", base.getServletRequest().getMethod());

		if(!uriInfo.getQueryParameters().isEmpty()) {
			builder.setHeader("query", uriInfo.getQueryParameters());
		}
		if(!uriInfo.getPathParameters().isEmpty()) {
			builder.setHeader("path", uriInfo.getPathParameters());
		}

		String user = getUserPrincipalName(base.getSecurityContext());
		if(StringUtils.isNotEmpty(user)) {
			builder.setHeader("issuedBy", user);
		}

		for(Entry<String, Object> customHeader : customHeaders.entrySet()) {
			builder.setHeader(customHeader.getKey(), customHeader.getValue());
		}

		return builder.build();
	}

	private static String getUserPrincipalName(SecurityContext securityContext) {
		Principal principal = securityContext.getUserPrincipal();
		if(principal != null && StringUtils.isNotEmpty(principal.getName())) {
			return principal.getName();
		}
		return null;
	}
}
