package nl.nn.adapterframework.lifecycle;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationListener;

import nl.nn.adapterframework.configuration.ConfigurationMessageEvent;
import nl.nn.adapterframework.util.MessageKeeper;

public class MessageEventListener implements ApplicationListener<ApplicationMessageEvent> {
	private static final int MESSAGEKEEPER_SIZE = 10;
	private static final String ALL_CONFIGS_KEY = "*ALL*";

	private Map<String, MessageKeeper> messageKeepers = new HashMap<>();

	public MessageEventListener() {
		globalLog().setMaxSize(MESSAGEKEEPER_SIZE * 2);
	}

	/**
	 * Get MessageKeeper for the application. The MessageKeeper is not
	 * stored at the Configuration object instance to prevent messages being
	 * lost after configuration reload.
	 * @return MessageKeeper for the application
	 */
	public MessageKeeper getMessageKeeper() {
		return getMessageKeeper(ALL_CONFIGS_KEY);
	}

	/**
	 * Get MessageKeeper for a specific configuration. The MessageKeeper is not
	 * stored at the Configuration object instance to prevent messages being
	 * lost after configuration reload.
	 * @param configurationName configuration name to get the MessageKeeper object from
	 * @return MessageKeeper for specified configurations
	 */
	public MessageKeeper getMessageKeeper(String configurationName) {
		return messageKeepers.get(configurationName);
	}

	private MessageKeeper globalLog() {
		return configLog(ALL_CONFIGS_KEY);
	}

	private MessageKeeper configLog(String key) {
		MessageKeeper messageKeeper = messageKeepers.get(key);
		if (messageKeeper == null) {
			messageKeeper = new MessageKeeper(MESSAGEKEEPER_SIZE);
			messageKeepers.put(key, messageKeeper);
		}
		return messageKeeper;
	}

	@Override
	public void onApplicationEvent(ApplicationMessageEvent event) {
		if(event instanceof ConfigurationMessageEvent) {
			String configurationName = ((ConfigurationMessageEvent) event).getSource().getName();
			configLog(configurationName).add(event.getMessageKeeperMessage());
		}
		globalLog().add(event.getMessageKeeperMessage());

		System.out.println(event.getMessageKeeperMessage());
	}
}
