/*
   Copyright 2023 WeAreFrank!

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
package org.frankframework.management.gateway;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.messaging.Message;

import lombok.Setter;

import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.util.SpringUtils;

public class HttpOutboundGateway implements InitializingBean, ApplicationContextAware, OutboundGateway {

	private HttpOutboundHandler handler;
	private @Setter ApplicationContext applicationContext;

	@Value("${management.gateway.http.outbound.endpoint}")
	private String endpoint;

	@Override
	public void afterPropertiesSet() throws Exception {
		if(StringUtils.isBlank(endpoint)) {
			throw new IllegalStateException("no endpoint specified");
		}

		handler = new HttpOutboundHandler(endpoint);
		SpringUtils.autowireByType(applicationContext, handler);
	}

	// I in, O out
	@Override
	@SuppressWarnings("unchecked")
	@Nonnull
	public <I, O> Message<O> sendSyncMessage(Message<I> in) {
		return (Message<O>) handler.handleRequestMessage(in);
	}

	// I in, no reply
	@Override
	public <I> void sendAsyncMessage(Message<I> in) {
		handler.handleRequestMessage(in);
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.outbound_gateway;
	}
}
