package org.frankframework.testutil;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationListener;

import org.frankframework.lifecycle.events.MessageEvent;
import org.frankframework.util.MessageKeeperMessage;
import org.frankframework.util.SizeLimitedVector;

/**
 * A Spring ApplicationListener that listens for MessageEvent events and stores them in a SizeLimitedVector.
 * This class is only for testing purposes, allowing you to verify that certain messages were emitted during the execution of your code.
 *
 * Note that the SizeLimitedVector will only keep the most recent 16 events, using FIFO the oldest ones will be discarded first.
 */
public class MessageEventListener implements ApplicationListener<MessageEvent<?>> {

	private final SizeLimitedVector<MessageEvent<?>> events = new SizeLimitedVector<>(16);

	@Override
	public void onApplicationEvent(@NonNull MessageEvent<?> event) {
		events.add(event);
	}

	/**
	 * Returns a list of messages for the events that are thrown by the specified class type.
	 */
	public List<String> getEvents(Class<? extends MessageEvent<?>> clazz) {
		return events.stream()
				.filter(clazz::isInstance)
				.map(MessageKeeperMessage::fromEvent)
				.map(MessageKeeperMessage::getMessageText)
				.toList();
	}

	public void clear() {
		events.clear();
	}
}
