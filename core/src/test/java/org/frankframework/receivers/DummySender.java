package org.frankframework.receivers;

import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.stream.Message;
import org.springframework.context.ApplicationContext;

public class DummySender implements ISender {
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;
	private @Getter @Setter String name;
	private @Getter boolean closed = false;

	@Override
	public void configure() throws ConfigurationException {
		//Nothing to configure
	}

	@Override
	public void open() {
		closed = false;
	}

	@Override
	public void close() {
		closed = true;
	}

	@Override
	public boolean isSynchronous() { // This method is used during tests to check if close is called.
		return isClosed();
	}

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		return new SenderResult(message);
	}
}
