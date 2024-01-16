package org.frankframework.testutil;

import java.util.ArrayList;
import java.util.List;

import org.frankframework.configuration.ConfigurationMessageEvent;
import org.frankframework.lifecycle.ApplicationMessageEvent;
import org.frankframework.util.MessageKeeperMessage;
import org.springframework.context.ApplicationListener;

public class ConfigurationMessageEventListener implements ApplicationListener<ApplicationMessageEvent> {
	private final List<MessageKeeperMessage> configurationMessages = new ArrayList<>();

	@Override
	public void onApplicationEvent(ApplicationMessageEvent event) {
		if(event instanceof ConfigurationMessageEvent) {
			configurationMessages.add(event.getMessageKeeperMessage());
		}
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
