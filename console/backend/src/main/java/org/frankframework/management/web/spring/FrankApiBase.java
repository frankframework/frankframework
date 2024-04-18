package org.frankframework.management.web.spring;

import lombok.Getter;
import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.web.ApiException;
import org.frankframework.util.ResponseUtils;
import org.frankframework.web.filters.DeprecationFilter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;

import javax.servlet.http.HttpServletRequest;

public abstract class FrankApiBase implements ApplicationContextAware, InitializingBean {

	@Autowired
	protected @Getter HttpServletRequest servletRequest;

	private @Getter ApplicationContext applicationContext;
	private @Getter Environment environment;

	protected Logger log = LogManager.getLogger(this);

	protected final OutboundGateway getGateway() {
		return getApplicationContext().getBean("outboundGateway", OutboundGateway.class);
	}

	@NonNull
	protected Message<?> sendSyncMessage(RequestMessageBuilder input) {
		Message<?> message = getGateway().sendSyncMessage(input.build());
		if(message == null) {
			StringBuilder errorMessage = new StringBuilder("did not receive a reply while sending message to topic ["+input.getTopic()+"]");
			if(input.getAction() != null) {
				errorMessage.append(" with action [");
				errorMessage.append(input.getAction());
				errorMessage.append("]");
			}
			throw new ApiException(errorMessage.toString());
		}
		return message;
	}

	public ResponseEntity<?> callSyncGateway(RequestMessageBuilder input) throws ApiException {
		Message<?> response = sendSyncMessage(input);
		// Build the reponse or do some final checks / return a different response
		return ResponseUtils.convertToSpringResponse(response);
	}

	public ResponseEntity<?> callAsyncGateway(RequestMessageBuilder input) {
		OutboundGateway gateway = getGateway();
		gateway.sendAsyncMessage(input.build());
		return ResponseEntity.ok().build();
	}

	@Override
	public final void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public final void afterPropertiesSet() {
		environment = applicationContext.getEnvironment();
	}

	/** Get a property from the Spring Environment. */
	protected <T> T getProperty(String key, T defaultValue) {
		return environment.getProperty(key, (Class<T>) defaultValue.getClass(), defaultValue);
	}

	protected final boolean allowDeprecatedEndpoints() {
		return getProperty(DeprecationFilter.ALLOW_DEPRECATED_ENDPOINTS_KEY, false);
	}
}
