package org.frankframework.monitoring;

import java.io.IOException;

import jakarta.annotation.Nonnull;

import lombok.Getter;
import lombok.SneakyThrows;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.senders.EchoSender;
import org.frankframework.stream.Message;

class MessageCapturingEchoSender extends EchoSender {
	private @Getter Message inputMessage;
	private @Getter PipeLineSession inputSession;
	private @Getter String sessionOriginalMessageValue;

	@Override
	@SneakyThrows(IOException.class)
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		inputMessage = message;
		inputSession = session;
		sessionOriginalMessageValue = session.getMessage(PipeLineSession.ORIGINAL_MESSAGE_KEY).asString();
		return super.sendMessage(message, session);
	}
}
