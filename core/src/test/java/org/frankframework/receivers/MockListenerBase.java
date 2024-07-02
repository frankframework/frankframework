package org.frankframework.receivers;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;

public abstract class MockListenerBase implements IListener<String> {
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter String name;
	private @Getter @Setter ApplicationContext applicationContext;
	private final AtomicBoolean isOpen = new AtomicBoolean(false);

	@Override
	public void configure() throws ConfigurationException {
		// Nothing to configure
	}

	/**
	 * If text equals <code>getRawMessageException</code> it throws an exception during the availability check.
	 * If text equals <code>extractMessageException</code> it throws an exception during unwrapping.
	 * If text equals <code>processMessageException</code> it throws an exception during message processing (in adapter).
	 */
	abstract void offerMessage(String text) throws Exception;

	@Override
	public void open() throws ListenerException {
		if(!isOpen.compareAndSet(false, true)) {
			throw new ListenerException("not in state closed");
		}
	}

	@Override
	public void close() throws ListenerException {
		if(!isOpen.compareAndSet(true, false)) {
			throw new ListenerException("not in state open");
		}
	}

	@Override
	public Message extractMessage(RawMessageWrapper<String> rawMessage, Map<String, Object> context) throws ListenerException {
		String text = rawMessage.getRawMessage();
		if("extractMessageException".equals(text)) {
			throw new ListenerException(text);
		}
		return new Message(text);
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<String> rawMessage, PipeLineSession pipeLineSession) throws ListenerException {
		// No-op
	}
}
