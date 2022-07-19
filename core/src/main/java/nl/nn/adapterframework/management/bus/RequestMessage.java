package nl.nn.adapterframework.management.bus;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.SecurityContext;

import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.support.GenericMessage;

import nl.nn.adapterframework.configuration.IbisManager.IbisAction;
import nl.nn.adapterframework.webcontrol.api.Base;

public class RequestMessage extends GenericMessage<IbisAction> {

	private static final long serialVersionUID = 4268801052358035098L;

	public RequestMessage(IbisAction action, Map<String, Object> headers) {
		super(action, headers);
	}

	public static RequestMessage create(Base base, IbisAction action) {
		String user = getUserPrincipalName(base.getSecurityContext());
		Map<String, Object> headers = new HashMap<>();
		if(StringUtils.isNotEmpty(user)) {
			headers.put("issuedBy", user);
		}
		return new RequestMessage(action, headers);
	}

	private static String getUserPrincipalName(SecurityContext securityContext) {
		Principal principal = securityContext.getUserPrincipal();
		if(principal != null && StringUtils.isNotEmpty(principal.getName())) {
			return principal.getName();
		}
		return null;
	}
}
