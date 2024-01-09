package org.frankframework.monitoring;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.senders.EchoSender;
import org.frankframework.stream.Message;

import lombok.Getter;
import lombok.SneakyThrows;

class MessageCapturingEchoSender extends EchoSender {
	private @Getter Message inputMessage;
	private @Getter PipeLineSession inputSession;
	private @Getter String sessionOriginalMessageValue;

	@Override
	@SneakyThrows
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		inputMessage = message;
		inputSession = session;
		sessionOriginalMessageValue = session.getMessage(PipeLineSession.ORIGINAL_MESSAGE_KEY).asString();
		return super.sendMessage(message, session);
	}
}
