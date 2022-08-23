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

import nl.nn.adapterframework.webcontrol.api.Base;

public class RequestMessageBuilder {
	private Map<String, Object> customHeaders = new HashMap<>();

	private final Base base;
	private final BusTopic topic;
	private Object payload = "NONE";

	public RequestMessageBuilder(Base base, BusTopic topic) {
		this.base = base;
		this.topic = topic;
	}

	public RequestMessageBuilder addHeader(String key, Object value) {
		customHeaders.put(key, value);
		return this;
	}

	public static RequestMessageBuilder create(Base base, BusTopic topic) {
		return new RequestMessageBuilder(base, topic);
	}

	public Message<?> build() {
		DefaultMessageBuilderFactory factory = base.getApplicationContext().getBean("messageBuilderFactory", DefaultMessageBuilderFactory.class);
		MessageBuilder<?> builder = factory.withPayload(payload);
		builder.setHeader(TopicSelector.TOPIC_HEADER_NAME, topic.name());

		UriInfo uriInfo = base.getUriInfo();
		builder.setHeader("uri", uriInfo.getRequestUri());
		builder.setHeader("method", base.getRequest().getMethod());

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
//		MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
//		builder.setHeader("query", queryParams);
//		for(Entry<String, List<String>> param : queryParams.entrySet()) {
//			builder.setHeader(param.getKey(), param.getValue());
//		}
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

	//public void handleAction(IbisAction action, String configurationName, String adapterName, String receiverName, String commandIssuedBy) {
}
