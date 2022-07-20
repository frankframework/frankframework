package nl.nn.adapterframework.management.bus;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.SecurityContext;

import org.apache.commons.lang3.StringUtils;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import nl.nn.adapterframework.configuration.IbisManager.IbisAction;
import nl.nn.adapterframework.util.SpringUtils;
import nl.nn.adapterframework.webcontrol.api.Base;

public class RequestMessage extends GenericMessage<IbisAction> {

	private static final long serialVersionUID = 1L;

	public RequestMessage(IbisAction action, Map<String, Object> headers) {
		super(action, headers);
	}

	public static Message<?> create(Base base, IbisAction action) {
		MessageBuilder<?> builder = base.getApplicationContext().getBean("messageBuilderFactory", MessageBuilder.class);
		String user = getUserPrincipalName(base.getSecurityContext());
		if(StringUtils.isNotEmpty(user)) {
			builder.setHeader("issuedBy", user);
		}
		builder.withPayload(action);
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
