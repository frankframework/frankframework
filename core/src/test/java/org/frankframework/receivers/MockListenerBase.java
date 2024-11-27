package org.frankframework.receivers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.Nonnull;

import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.stream.Message;

public abstract class MockListenerBase implements IListener<String> {
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter String name;
	private @Getter @Setter ApplicationContext applicationContext;
	private final AtomicBoolean isOpen = new AtomicBoolean(false);
	private final List<PipeLine.ExitState> exitStates = Collections.synchronizedList(new ArrayList<>());

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
	public void start() {
		if(!isOpen.compareAndSet(false, true)) {
			throw new LifecycleException("not in state closed");
		}
	}

	@Override
	public void stop() {
		if(!isOpen.compareAndSet(true, false)) {
			throw new LifecycleException("not in state open");
		}
	}

	@Override
	public Message extractMessage(RawMessageWrapper<String> rawMessage, @Nonnull Map<String, Object> context) throws ListenerException {
		String text = rawMessage.getRawMessage();
		if("extractMessageException".equals(text)) {
			throw new ListenerException(text);
		}
		return new Message(text);
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<String> rawMessage, PipeLineSession pipeLineSession) {
		exitStates.add(processResult.getState());
	}

	public PipeLine.ExitState getLastExitState() {
		return exitStates.get(exitStates.size() - 1);
	}
}
