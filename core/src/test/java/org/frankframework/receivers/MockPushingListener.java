package org.frankframework.receivers;

import lombok.Setter;

import org.frankframework.core.IKnowsDeliveryCount;
import org.frankframework.core.IMessageHandler;
import org.frankframework.core.IPushingListener;
import org.frankframework.core.IbisExceptionListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;

public class MockPushingListener extends MockListenerBase implements IPushingListener<String>, IKnowsDeliveryCount<String> {
	private @Setter IMessageHandler<String> handler;
	private @Setter IbisExceptionListener exceptionListener;
	private @Setter int mockedDeliveryCount;

	@Override
	public void offerMessage(String text) throws ListenerException {
		try(PipeLineSession session = new PipeLineSession()) {
			MessageWrapper<String> raw = wrapRawMessage(text, session);
			handler.processRequest(this, raw, session);
		}
	}

	@Override
	public MessageWrapper<String> wrapRawMessage(String rawMessage, PipeLineSession session) {
		return new MessageWrapper<>(new Message(rawMessage), session.getMessageId(), session.getCorrelationId());
	}

	@Override
	public int getDeliveryCount(RawMessageWrapper<String> rawMessage) {
		return mockedDeliveryCount;
	}
}
