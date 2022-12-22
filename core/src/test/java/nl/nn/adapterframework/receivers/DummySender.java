package nl.nn.adapterframework.receivers;

import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.stream.Message;

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
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		return new SenderResult(message);
	}
}
