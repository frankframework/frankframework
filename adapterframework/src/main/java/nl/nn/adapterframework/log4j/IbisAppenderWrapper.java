package nl.nn.adapterframework.extensions.log4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Extension of AppenderSkeleton with the facility to truncate all messages to a specified length.
 * 
 * @author Peter Leeuwenburgh
 * @version $Id$
 */

public class IbisAppenderWrapper extends AppenderSkeleton implements
		AppenderAttachable {
	protected int maxMessageLength = -1;

	private final List<Appender> appenders = new ArrayList<Appender>();

	public void close() {
		synchronized (appenders) {
			for (Appender appender : appenders) {
				appender.close();
			}
		}
	}

	public boolean requiresLayout() {
		return false;
	}

	@Override
	protected void append(LoggingEvent event) {
		String modifiedMessage = event.getMessage().toString();
		if (maxMessageLength >= 0
				&& modifiedMessage.length() > maxMessageLength) {
			modifiedMessage = modifiedMessage.substring(0, maxMessageLength) + "...(" + (modifiedMessage.length() - maxMessageLength) + " characters more)";
		}
		LoggingEvent modifiedEvent = new LoggingEvent(
				event.getFQNOfLoggerClass(), event.getLogger(),
				event.getTimeStamp(), event.getLevel(), modifiedMessage,
				event.getThreadName(), event.getThrowableInformation(),
				event.getNDC(), event.getLocationInformation(),
				event.getProperties());

		synchronized (appenders) {
			for (Appender appender : appenders) {
				appender.doAppend(modifiedEvent);
			}
		}
	}

	public void addAppender(Appender appender) {
		synchronized (appenders) {
			appenders.add(appender);
		}

	}

	public Enumeration getAllAppenders() {
		return Collections.enumeration(appenders);
	}

	public Appender getAppender(String name) {
		synchronized (appenders) {
			for (Appender appender : appenders) {
				if (appender.getName().equals(name)) {
					return appender;
				}
			}
		}
		return null;
	}

	public boolean isAttached(Appender appender) {
		synchronized (appenders) {
			for (Appender wrapped : appenders) {
				if (wrapped.equals(appender)) {
					return true;
				}
			}
			return false;
		}
	}

	public void removeAllAppenders() {
		synchronized (appenders) {
			appenders.clear();
		}
	}

	public void removeAppender(Appender appender) {
		synchronized (appenders) {
			for (Iterator<Appender> i = appenders.iterator(); i.hasNext();) {
				if (i.next().equals(appender)) {
					i.remove();
				}
			}
		}
	}

	public void removeAppender(String name) {
		synchronized (appenders) {
			for (Iterator<Appender> i = appenders.iterator(); i.hasNext();) {
				if (i.next().getName().equals(name)) {
					i.remove();
				}
			}
		}
	}

	public void setMaxMessageLength(int maxMessageLength) {
		this.maxMessageLength = maxMessageLength;
	}

	public int getMaxMessageLength() {
		return maxMessageLength;
	}
}