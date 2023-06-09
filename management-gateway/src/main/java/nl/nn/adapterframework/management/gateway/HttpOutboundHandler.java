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
package nl.nn.adapterframework.management.gateway;

import java.util.Collections;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import nl.nn.adapterframework.util.SpringUtils;

public class HttpOutboundHandler extends HttpRequestExecutingMessageHandler {

	public HttpOutboundHandler(String endpoint) {
		super(endpoint);
	}

	/**
	 * Triggered by final AfterPropertiesSet()
	 */
	@Override
	protected void doInit() {
		super.doInit();

		QueueChannel responseChannel = SpringUtils.createBean(getApplicationContext(), QueueChannel.class);
		setOutputChannel(responseChannel);

		DefaultHttpHeaderMapper headerMapper = SpringUtils.createBean(getApplicationContext(), DefaultHttpHeaderMapper.class);
		headerMapper.setOutboundHeaderNames("topic", "action");
		headerMapper.setInboundHeaderNames("meta-*");
		setHeaderMapper(headerMapper);

		setMessageConverters(Collections.singletonList(new ByteArrayHttpMessageConverter()));

		setHttpMethodExpression(new HttpMethodExpression());
	}

	private static class HttpMethodExpression extends ValueExpression<HttpMethod> {

		public HttpMethodExpression() {
			super(HttpMethod.POST);
		}

		@Override
		public HttpMethod getValue(EvaluationContext context, Object rootObject) throws EvaluationException {
			if(rootObject instanceof Message<?> && "NONE".equals(((Message<?>) rootObject).getPayload())) {
				return HttpMethod.GET;
			}
			return super.getValue(context, rootObject);
		}
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Message handleRequestMessage(Message<?> requestMessage) {
		Object response = super.handleRequestMessage(requestMessage);
		if(response instanceof Message) {
			return (Message) response;
		}
		if(response instanceof MessageBuilder) {
			return ((MessageBuilder) response).build();
		}

		throw new IllegalStateException("unknown response type ["+((response != null) ? response.getClass().getCanonicalName() : "null")+"]");
	}

	@Override
	protected Object resolveErrorChannel(MessageHeaders requestHeaders) {
		return getOutputChannel();
	}

	/**
	 * Always convert to a binary response, JAX-RS may convert this to characters if needed.
	 * The conversion from String to binary may use the wrong encoding.
	 */
	@Override
	protected Object evaluateTypeFromExpression(Message<?> requestMessage, Expression expression, String property) {
		if("expectedResponseType".equals(property)) {
			return byte[].class;
		}

		return super.evaluateTypeFromExpression(requestMessage, expression, property);
	}
}
