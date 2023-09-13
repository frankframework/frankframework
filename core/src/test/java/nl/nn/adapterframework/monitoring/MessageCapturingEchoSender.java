package nl.nn.adapterframework.monitoring;

import lombok.Getter;
import lombok.SneakyThrows;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.senders.EchoSender;
import nl.nn.adapterframework.stream.Message;

class MessageCapturingEchoSender extends EchoSender {
	private @Getter Message inputMessage;
	private @Getter PipeLineSession inputSession;
	private @Getter String sessionOriginalMessageValue;

	@Override
	@SneakyThrows
	public Message sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		inputMessage = message;
		inputSession = session;
		sessionOriginalMessageValue = session.getMessage(PipeLineSession.originalMessageKey).asString();
		return super.sendMessage(message, session);
	}
}
