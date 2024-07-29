/*
   Copyright 2024 WeAreFrank!

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.web.configuration.DeprecationInterceptor;
import org.frankframework.management.web.socket.MessageCacheStore;
import org.frankframework.util.ResponseUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;

import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;

public abstract class FrankApiBase implements ApplicationContextAware, InitializingBean {

	@Autowired
	protected @Getter MessageCacheStore messageCacheStore;

	@Autowired
	protected @Getter HttpServletRequest servletRequest;
	protected Logger log = LogManager.getLogger(this);
	protected final OutboundGateway getGateway() {
		return getApplicationContext().getBean("outboundGateway", OutboundGateway.class);
	}

	private @Getter ApplicationContext applicationContext;
	private @Getter Environment environment;

	@Nonnull
	protected Message<?> sendSyncMessage(RequestMessageBuilder input) {
		Message<?> message = getGateway().sendSyncMessage(input.build());
		if (message == null) {
			throw createErrorMessage(input.getTopic(), input.getAction());
		}
		return message;
	}

	public ResponseEntity<?> callSyncGateway(RequestMessageBuilder input) throws ApiException {
		Message<?> response = sendSyncMessage(input);
		// Build the response or do some final checks / return a different response
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
		return getProperty(DeprecationInterceptor.ALLOW_DEPRECATED_ENDPOINTS_KEY, false);
	}

	protected ApiException createErrorMessage(BusTopic topic, BusAction action) {
		StringBuilder errorMessage = new StringBuilder("did not receive a reply while sending message to topic [" + topic + "]");
		if (action != null) {
			errorMessage.append(" with action [");
			errorMessage.append(action);
			errorMessage.append("]");
		}
		return new ApiException(errorMessage.toString());
	}
}
