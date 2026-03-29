package org.frankframework.testutil;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationListener;

import org.frankframework.lifecycle.events.MessageEvent;
import org.frankframework.util.MessageKeeperMessage;
import org.frankframework.util.SizeLimitedVector;

public class MessageEventListener implements ApplicationListener<MessageEvent<?>> {

	private final SizeLimitedVector<MessageEvent<?>> events = new SizeLimitedVector<>(16);

	@Override
	public void onApplicationEvent(@NonNull MessageEvent<?> event) {
		events.add(event);
	}

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
