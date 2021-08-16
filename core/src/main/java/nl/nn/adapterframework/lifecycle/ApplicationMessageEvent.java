package nl.nn.adapterframework.lifecycle;

import java.util.Date;

import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;

import lombok.Getter;
import nl.nn.adapterframework.util.MessageKeeper.MessageKeeperLevel;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeperMessage;

public class ApplicationMessageEvent extends ApplicationContextEvent {
	private @Getter MessageKeeperMessage messageKeeperMessage;
	protected final Logger log = LogUtil.getLogger(ApplicationMessageEvent.class);

	protected ApplicationMessageEvent(ApplicationContext source) {
		super(source);
	}

	public ApplicationMessageEvent(ApplicationContext source, String message) {
		this(source, message, MessageKeeperLevel.INFO);
	}

	public ApplicationMessageEvent(ApplicationContext source, String message, MessageKeeperLevel level) {
		this(source, message, level, null);
	}

	public ApplicationMessageEvent(ApplicationContext source, String message, Exception e) {
		this(source, message, MessageKeeperLevel.ERROR, e);
	}

	public ApplicationMessageEvent(ApplicationContext source, String message, MessageKeeperLevel level, Exception e) {
		this(source);

		StringBuilder m = new StringBuilder();

		String applicationName = source.getId();
		m.append("Application [" + applicationName + "] ");

		String version = ConfigurationUtils.getApplicationVersion();
		if (version != null) {
			m.append("[" + version + "] ");
		}

		if (MessageKeeperLevel.ERROR.equals(level)) {
			log.error(m, e);
		} else if (MessageKeeperLevel.WARN.equals(level)) {
			log.warn(m, e);
		} else {
			log.info(m, e);
		}

		m.append(message);

		if (e != null) {
			m.append(": " + e.getMessage());
		}

		Date date = new Date(getTimestamp());
		messageKeeperMessage = new MessageKeeperMessage(m.toString(), date, level);
	}
}
