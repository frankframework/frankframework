package nl.nn.adapterframework.testutil;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationListener;

import nl.nn.adapterframework.configuration.ConfigurationMessageEvent;
import nl.nn.adapterframework.lifecycle.ApplicationMessageEvent;
import nl.nn.adapterframework.util.MessageKeeperMessage;

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
