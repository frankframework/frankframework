package org.frankframework.receivers;

import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IKnowsDeliveryCount;
import org.frankframework.core.IMessageHandler;
import org.frankframework.core.IPushingListener;
import org.frankframework.core.IbisExceptionListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;

/**
 * This listener roughly imitates how a JMS listener handles retry-count and message conversion, and is used in tests to make sure that
 * retried messages from an error-storage are not passed to this listener when formats are incompatible.
 */
public class MockPushingListenerWithCustomMessageType implements IPushingListener<MockPushingListenerWithCustomMessageType.CustomMessageClass>, IKnowsDeliveryCount<MockPushingListenerWithCustomMessageType.CustomMessageClass> {

	private String name;
	private ApplicationContext applicationContext;

	@Override
	public int getDeliveryCount(RawMessageWrapper<CustomMessageClass> rawMessage) {
		return rawMessage.getRawMessage().getDeliveryCount();
	}

	@Override
	public void setHandler(IMessageHandler<CustomMessageClass> handler) {
		// No-op
	}

	@Override
	public void setExceptionListener(IbisExceptionListener listener) {
		// No-op
	}

	@Override
	public RawMessageWrapper<CustomMessageClass> wrapRawMessage(CustomMessageClass rawMessage, PipeLineSession session) throws ListenerException {
		return new RawMessageWrapper<>(rawMessage);
	}

	@Override
	public void start() {
		// No-op
	}

	@Override
	public void stop() {
		// No-op
	}

	@Override
	public Message extractMessage(@NonNull RawMessageWrapper<CustomMessageClass> rawMessage, @NonNull Map<String, Object> context) throws ListenerException {
		return Message.asMessage(rawMessage.rawMessage.getMessage());
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<CustomMessageClass> rawMessage, PipeLineSession pipeLineSession) throws ListenerException {
		// No-op
	}

	@Override
	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void configure() throws ConfigurationException {
		// No-op
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public boolean messageWillBeRedeliveredOnExitStateError() {
		return true;
	}

	public static class CustomMessageClass {
		private final @Getter String message;
		private int deliveryCount;

		public CustomMessageClass(String message) {
			this.message = message;
		}

		public int getDeliveryCount() {
			return ++deliveryCount;
		}
	}
}
