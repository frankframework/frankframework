package nl.nn.adapterframework.receivers;

import lombok.Setter;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;

public class MockPushingListener extends MockListenerBase implements IPushingListener<String> {
	private @Setter IMessageHandler<String> handler;
	private @Setter IbisExceptionListener exceptionListener;

	@Override
	public void offerMessage(String text) throws ListenerException {
		try(PipeLineSession session = new PipeLineSession()) {
			RawMessageWrapper<String> raw = wrapRawMessage(text, session);
			handler.processRequest(this, raw, Message.asMessage(text), session);
		}
	}

	@Override
	public RawMessageWrapper<String> wrapRawMessage(String rawMessage, PipeLineSession session) throws ListenerException {
		return new RawMessageWrapper<>(rawMessage, session.getMessageId(), session.getCorrelationId());
	}
}
