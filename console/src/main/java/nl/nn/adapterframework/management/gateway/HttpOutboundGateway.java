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

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.http.HttpMethod;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.util.Assert;

import nl.nn.adapterframework.management.bus.IntegrationGateway;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.SpringUtils;

public class HttpOutboundGateway<T> extends HttpRequestExecutingMessageHandler implements IntegrationGateway<T> {
	private static final String HTTP_ENDPOINT = AppConstants.getInstance().getString("management.http.endpoint", "http://localhost/iaf-test/iaf/management");

	public HttpOutboundGateway() {
		super(HTTP_ENDPOINT);
	}

	@Override
	protected void doInit() {
		super.doInit();

		QueueChannel responseChannel = SpringUtils.createBean(getApplicationContext(), QueueChannel.class);
		setOutputChannel(responseChannel);

//		resolveErrorChannel(null)
//		setErrorHandler(null);
		DefaultHttpHeaderMapper headerMapper = SpringUtils.createBean(getApplicationContext(), DefaultHttpHeaderMapper.class);
		headerMapper.setOutboundHeaderNames("topic", "action");
		headerMapper.setInboundHeaderNames("meta-*");
		setHeaderMapper(headerMapper);

		setHttpMethodExpression(new HttpMethodExpression());
	}

	private static class HttpMethodExpression extends ValueExpression<HttpMethod> {

		public HttpMethodExpression() {
			super(HttpMethod.POST);
		}

		@Override
		public HttpMethod getValue(EvaluationContext context, Object rootObject) throws EvaluationException {
			if(rootObject instanceof Message<?> && ((Message<?>) rootObject).getPayload() == "NONE") {
				return HttpMethod.GET;
			}
			return super.getValue(context, rootObject);
		}
	}

	@Override //Is dit handig?
	protected Object resolveErrorChannel(MessageHeaders requestHeaders) {
		return getOutputChannel();
	}

	// T in T out.
	@Override//handleRequestMessage
	public Message<T> sendSyncMessage(Message<T> in) {
//		try {
//			super.handleMessage(in);
//		} catch (MessageHandlingException e) {
//			e.printStackTrace();
//		}
		Object response = handleRequestMessage(in);
		if(response instanceof Message) {
			return (Message<T>) response;
		}
		if(response instanceof MessageBuilder) {
			return ((MessageBuilder) response).build();
		}

		System.out.println("null!!?? " + response);
		return null;
//		return (Message<T>) doReceive(getOutputChannel(), 5000);
	}

	@Override
	protected Object evaluateTypeFromExpression(Message<?> requestMessage, Expression expression, String property) {
		System.out.println("expression: " + expression);
		if("expectedResponseType".equals(property)) {
			return String.class;
		}

		return super.evaluateTypeFromExpression(requestMessage, expression, property);
	}

	@Override
	public void onComplete() {
		System.err.println("wat doet dit");
		super.onComplete();
	}

	@Override
	protected void sendOutputs(Object result, Message<?> requestMessage) {
		System.out.println("send output" + result);
		super.sendOutputs(result, requestMessage);
	}

//	@Override
//	protected void sendOutput(Object output, Object replyChannelArg, boolean useArgChannel) {
//		queue.add(output);
//	}

	@Nullable
	private final Message<?> doReceive(MessageChannel channel, long timeout) {
		Assert.notNull(channel, "MessageChannel is required");
		Assert.state(channel instanceof PollableChannel, "A PollableChannel is required to receive messages");

		Message<?> message = (timeout >= 0 ? ((PollableChannel) channel).receive(timeout) : ((PollableChannel) channel).receive());

		if (message == null && logger.isTraceEnabled()) {
			logger.trace("Failed to receive message from channel [" + channel + "] within timeout [" + timeout+"]");
		}

		return message;
	}


//	public Object waitForResponse() throws BusException {
//		try {
//			final Object v = queue.poll(5, TimeUnit.SECONDS); //Shouldn't take longer then 50 ms, but just to be sure..
//			if(v == null) {
//				throw new BusException("Timeout exceeded");
//			}
//			return v;
//		} catch (InterruptedException e) {
//			throw new BusException("Waiting for result interrupted", e);
//		}
//	}

	// T in, no reply
	@Override
	public void sendAsyncMessage(Message<T> in) {
		super.handleMessage(in);
	}
}
