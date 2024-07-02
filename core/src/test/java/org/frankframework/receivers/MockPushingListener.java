package org.frankframework.receivers;

import org.frankframework.core.IMessageHandler;
import org.frankframework.core.IPushingListener;
import org.frankframework.core.IbisExceptionListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;

import lombok.Setter;

public class MockPushingListener extends MockListenerBase implements IPushingListener<String> {
	private @Setter IMessageHandler<String> handler;
	private @Setter IbisExceptionListener exceptionListener;

	@Override
	public void offerMessage(String text) throws ListenerException {
		try(PipeLineSession session = new PipeLineSession()) {
			RawMessageWrapper<String> raw = wrapRawMessage(text, session);
			handler.processRequest(this, raw, new Message(text), session);
		}
	}

	@Override
	public RawMessageWrapper<String> wrapRawMessage(String rawMessage, PipeLineSession session) throws ListenerException {
		return new RawMessageWrapper<>(rawMessage, session.getMessageId(), session.getCorrelationId());
	}
}
