package org.frankframework.testutil;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationListener;

import org.frankframework.lifecycle.events.ConfigurationMessageEvent;
import org.frankframework.util.MessageKeeperMessage;

public class ConfigurationMessageEventListener implements ApplicationListener<ConfigurationMessageEvent> {
	private final List<MessageKeeperMessage> configurationMessages = new ArrayList<>();

	@Override
	public void onApplicationEvent(ConfigurationMessageEvent event) {
		MessageKeeperMessage messageKeeperMessage = MessageKeeperMessage.fromEvent(event);
		configurationMessages.add(messageKeeperMessage);
	}

	public boolean contains(String message) {
		for(MessageKeeperMessage mkm : configurationMessages) {
			if(mkm.getMessageText().contains(message)) {
				return true;
			}
		}
		return false;
	}
}
