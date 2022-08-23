package nl.nn.adapterframework.management.bus;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import nl.nn.adapterframework.configuration.IbisManager.IbisAction;
import nl.nn.adapterframework.webcontrol.api.Base;

public class RequestMessage {

	private static final long serialVersionUID = 1L;

	public static Message<?> create(Base base, BusTopic action) {
		DefaultMessageBuilderFactory factory = base.getApplicationContext().getBean("messageBuilderFactory", DefaultMessageBuilderFactory.class);
		MessageBuilder<String> builder = factory.withPayload("");
		builder.setHeader("action", action.name());

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
