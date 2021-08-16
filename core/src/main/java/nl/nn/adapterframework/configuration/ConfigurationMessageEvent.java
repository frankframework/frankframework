package nl.nn.adapterframework.configuration;

import java.util.Date;

import org.springframework.context.ApplicationContext;

import lombok.Getter;
import nl.nn.adapterframework.core.IConfigurationAware;
import nl.nn.adapterframework.lifecycle.ApplicationMessageEvent;
import nl.nn.adapterframework.util.MessageKeeper.MessageKeeperLevel;
import nl.nn.adapterframework.util.MessageKeeperMessage;

public class ConfigurationMessageEvent extends ApplicationMessageEvent {
	private @Getter MessageKeeperMessage messageKeeperMessage;

	public ConfigurationMessageEvent(IConfigurationAware source, String message) {
		this(source, message, MessageKeeperLevel.INFO);
	}

	public ConfigurationMessageEvent(IConfigurationAware source, String message, MessageKeeperLevel level) {
		this(getSource(source), message, level, null);
	}

	public ConfigurationMessageEvent(IConfigurationAware source, String message, Exception e) {
		this(getSource(source), message, MessageKeeperLevel.ERROR, e);
	}

	private static ApplicationContext getSource(IConfigurationAware source) {
		ApplicationContext ac = source.getApplicationContext();
		if(ac instanceof Configuration) {
			return ac;
		}
		throw new IllegalStateException("unable to publish message from this context");
	}

	@Override
	public Configuration getSource() {
		return (Configuration) super.getSource();
	}

	private ConfigurationMessageEvent(ApplicationContext source, String message, MessageKeeperLevel level, Exception e) {
		super(source);
		StringBuilder m = new StringBuilder();

		String configurationName = getSource().getName();
		m.append("Configuration [" + configurationName + "] ");

		String version = getSource().getVersion();
		if (version != null) {
			m.append("[" + version + "] ");
		}

		m.append(message);

		if (MessageKeeperLevel.ERROR.equals(level)) {
			log.error(m, e);
		} else if (MessageKeeperLevel.WARN.equals(level)) {
			log.warn(m, e);
		} else {
			log.info(m, e);
		}

		if (e != null) {
			m.append(": " + e.getMessage());
		}

		Date date = new Date(getTimestamp());
		messageKeeperMessage = new MessageKeeperMessage(m.toString(), date, level);
	}
}
