package nl.nn.adapterframework.extensions.log4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

/**
 * Extension of AppenderSkeleton with the facility to truncate all messages to a specified length.
 * 
 * @author Peter Leeuwenburgh
 */

public class IbisAppenderWrapper extends AppenderSkeleton implements
		AppenderAttachable {
	protected int maxMessageLength = -1;
	protected String hideRegex;
	
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
		String modifiedMessage = event.getMessage()==null?"":event.getMessage().toString();
		if (maxMessageLength >= 0 && modifiedMessage.length() > maxMessageLength) {
			modifiedMessage = modifiedMessage.substring(0, maxMessageLength) + "...(" + (modifiedMessage.length() - maxMessageLength) + " characters more)";
		}

		String throwableStrReps[] = null;
		ThrowableInformation throwableInfo = event.getThrowableInformation();
		if (throwableInfo!=null) {
			throwableStrReps = throwableInfo.getThrowableStrRep();
		}

		if (StringUtils.isNotEmpty(hideRegex)) {
			modifiedMessage = Misc.hideAll(modifiedMessage, hideRegex);

			if (throwableStrReps!=null) {
				for (int i=0; i<throwableStrReps.length; i++) {
					throwableStrReps[i] = Misc.hideAll(throwableStrReps[i], hideRegex);
				}
			}
		}

		String threadHideRegex = LogUtil.getThreadHideRegex();
		if (StringUtils.isNotEmpty(threadHideRegex)) {
			modifiedMessage = Misc.hideAll(modifiedMessage, threadHideRegex);

			if (throwableStrReps!=null) {
				for (int i=0; i<throwableStrReps.length; i++) {
					throwableStrReps[i] = Misc.hideAll(throwableStrReps[i], threadHideRegex);
				}
			}
		}
		
		LoggingEvent modifiedEvent = new LoggingEvent(
				event.getFQNOfLoggerClass(), event.getLogger(),
				event.getTimeStamp(), event.getLevel(), modifiedMessage,
				event.getThreadName(), new ThrowableInformation(throwableStrReps),
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

	public String getHideRegex() {
		return hideRegex;
	}

	public void setHideRegex(String string) {
		hideRegex = string;
	}
}