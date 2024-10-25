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

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.security.JwtKeyGenerator;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.StreamUtil;

public class HttpOutboundHandler extends HttpRequestExecutingMessageHandler {

	@Autowired
	private JwtKeyGenerator jwtGenerator;

	public HttpOutboundHandler(String endpoint) {
		super(endpoint);
	}

	/**
	 * Triggered by final AfterPropertiesSet()
	 */
	@Override
	protected void doInit() {
		if(jwtGenerator == null) {
			throw new IllegalStateException("JwtKeyGenerator not set");
		}

		super.doInit();

		QueueChannel responseChannel = SpringUtils.createBean(getApplicationContext(), QueueChannel.class);
		setOutputChannel(responseChannel);

		DefaultHttpHeaderMapper headerMapper = SpringUtils.createBean(getApplicationContext(), DefaultHttpHeaderMapper.class);
		headerMapper.setOutboundHeaderNames(getRequestHeaders());
		headerMapper.setInboundHeaderNames(BusMessageUtils.HEADER_PREFIX_PATTERN);
		setHeaderMapper(headerMapper);

		setMessageConverters(getMessageConverters());

		setHttpMethodExpression(new HttpMethodExpression());
	}

	/**
	 * Reply converters to turn byte[] / InputStreams and Strings to something that the HTTP Inbound and Outbound gateways can understand.
	 */
	private List<HttpMessageConverter<?>> getMessageConverters() {
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
		StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter(StreamUtil.DEFAULT_CHARSET);
		stringHttpMessageConverter.setWriteAcceptCharset(false);
		messageConverters.add(stringHttpMessageConverter);
		messageConverters.add(new InputStreamHttpMessageConverter());
		messageConverters.add(new ByteArrayHttpMessageConverter());
		return messageConverters;
	}

	private String[] getRequestHeaders() {
		List<String> headers = new ArrayList<>();
		headers.add(BusAction.ACTION_HEADER_NAME);
		headers.add(BusTopic.TOPIC_HEADER_NAME);
		headers.add(BusMessageUtils.HEADER_TARGET_KEY);
		headers.add(BusMessageUtils.HEADER_PREFIX_PATTERN);
		return headers.toArray(new String[0]);
	}

	private static class HttpMethodExpression extends ValueExpression<HttpMethod> {

		public HttpMethodExpression() {
			super(HttpMethod.POST);
		}

		@Override
		public HttpMethod getValue(EvaluationContext context, Object rootObject) throws EvaluationException {
			if(rootObject instanceof Message<?> message && "NONE".equals(message.getPayload())) {
				return HttpMethod.GET;
			}
			return super.getValue(context, rootObject);
		}
	}

	/**
	 * Add authentication JWT, see {@link JwtKeyGenerator}.
	 */
	@Override
	protected HttpHeaders mapHeaders(Message<?> message) {
		HttpHeaders headers = super.mapHeaders(message);

		headers.add("Authentication", "Bearer " + jwtGenerator.create());
		return headers;
	}

	@Override
	@Nonnull
	@SuppressWarnings("rawtypes")
	public Message handleRequestMessage(Message<?> requestMessage) {
		Object response = super.handleRequestMessage(requestMessage);

		if(response instanceof Message message) {
			return message;
		}
		if(response instanceof MessageBuilder builder) {
			return builder.build();
		}
		throw new BusException("unknown response type ["+(response != null ? response.getClass().getCanonicalName() : "null")+"]");
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
