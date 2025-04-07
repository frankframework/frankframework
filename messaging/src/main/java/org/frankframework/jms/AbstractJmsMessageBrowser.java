/*
   Copyright 2013 Nationale-Nederlanden, 2020-2023 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.jms;

import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.IMessageBrowser;
import org.frankframework.core.IMessageBrowsingIterator;
import org.frankframework.core.IMessageBrowsingIteratorItem;
import org.frankframework.core.ListenerException;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.StringUtil;

/**
 * Basic browser of JMS Messages.
 * @param <M> the payload message type as used by IMessageBrowser.
 * @param <J> the physical JMS message to carry the payload.
 *
 * {@inheritClassDoc}
 *
 * @author  Johan Verrips
 */
public abstract class AbstractJmsMessageBrowser<M, J extends jakarta.jms.Message> extends JMSFacade implements IMessageBrowser<M> {

	private @Getter long timeout = 3000;
	private @Getter String selector=null;

	private @Getter @Setter String hideRegex = null;
	private @Getter @Setter HideMethod hideMethod = HideMethod.ALL;

	public AbstractJmsMessageBrowser() {
		super();
		setTransacted(true);
	}

	public AbstractJmsMessageBrowser(String selector) {
		this();
		this.selector=selector;
	}

	@Override
	public IMessageBrowsingIterator getIterator() throws ListenerException {
		try {
			return new JmsQueueBrowserIterator(this,(Queue)getDestination(),getSelector());
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public IMessageBrowsingIterator getIterator(Date startTime, Date endTime, SortOrder order) throws ListenerException {
		String selector=getSelector();
		if (startTime!=null) {
			selector= StringUtil.concatStrings(selector, " AND ", "JMSTimestamp >= "+ DateFormatUtils.format(startTime));
		}
		if (endTime!=null) {
			selector= StringUtil.concatStrings(selector, " AND ", "JMSTimestamp < "+ DateFormatUtils.format(endTime));
		}
		try {
			return new JmsQueueBrowserIterator(this,(Queue)getDestination(),selector);
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public boolean containsMessageId(String originalMessageId) throws ListenerException {
		Object msg = browseJmsMessage(originalMessageId);
		return msg != null;
	}

	@Override
	public boolean containsCorrelationId(String correlationId) throws ListenerException {
		log.warn("could not determine correct presence of a message with correlationId [{}], assuming it does not exist", correlationId);
		// TODO: check presence of a message with correlationId
		return false;
	}

	@Override
	public int getMessageCount() throws ListenerException {
		QueueBrowser queueBrowser=null;
		Session session = null;
		try {
			session = createSession();
			if (StringUtils.isEmpty(getSelector())) {
				queueBrowser=session.createBrowser((Queue)getDestination());
			} else {
				queueBrowser=session.createBrowser((Queue)getDestination(), getSelector());
			}
			int count=0;
			for (Enumeration<?> enm=queueBrowser.getEnumeration(); enm.hasMoreElements(); enm.nextElement()) {
				count++;
			}
			return count;
		} catch (Exception e) {
			throw new ListenerException("cannot determin messagecount",e);
		} finally {
			try {
				if (queueBrowser!=null) {
					queueBrowser.close();
				}
			} catch (JMSException e) {
				throw new ListenerException("error closing queuebrowser",e);
			}
			closeSession(session);
		}
	}

	public J getJmsMessage(String messageId) throws ListenerException {
		Session session=null;
		J msg;
		MessageConsumer mc = null;
		try {
			session = createSession();
			mc = getMessageConsumer(session, getDestination(), getCombinedSelector(messageId));
			msg = (J)mc.receive(getTimeout());
			return msg;
		} catch (Exception e) {
			throw new ListenerException(e);
		} finally {
			try {
				if (mc != null) {
					mc.close();
				}
			} catch (JMSException e1) {
				throw new ListenerException("exception closing message consumer",e1);
			}
			closeSession(session);
		}
	}

	@Override
	public IMessageBrowsingIteratorItem getContext(String messageId) throws ListenerException {
		return new JmsMessageBrowserIteratorItem(doBrowse("JMSMessageID", messageId));
	}

	public J browseJmsMessage(String messageId) throws ListenerException {
		return (J)doBrowse("JMSMessageID", messageId);
	}


	protected jakarta.jms.Message doBrowse(Map<String,String> selectors) throws ListenerException {
		QueueSession session=null;
		QueueBrowser queueBrowser=null;
		try {
			session = (QueueSession)createSession();
			queueBrowser = session.createBrowser((Queue)getDestination(),getCombinedSelector(selectors));
			Enumeration<?> msgenum = queueBrowser.getEnumeration();
			if (msgenum.hasMoreElements()) {
				return (jakarta.jms.Message) msgenum.nextElement();
			}
			return null;
		} catch (Exception e) {
			throw new ListenerException(e);
		} finally {
			try {
				if (queueBrowser != null) {
					queueBrowser.close();
				}
			} catch (JMSException e1) {
				throw new ListenerException("exception closing queueBrowser",e1);
			}
			closeSession(session);
		}
	}

	protected jakarta.jms.Message doBrowse(String selectorKey, String selectorValue) throws ListenerException {
		Map<String,String> selectorMap = new HashMap<>();
		selectorMap.put(selectorKey, selectorValue);
		return doBrowse(selectorMap);
	}

	@Override
	public void deleteMessage(String messageId) throws ListenerException {
		Session session=null;
		MessageConsumer mc = null;
		try {
			session = createSession();
			log.debug("retrieving message [{}] in order to delete it", messageId);
			mc = getMessageConsumer(session, getDestination(), getCombinedSelector(messageId));
			mc.receive(getTimeout());
		} catch (Exception e) {
			throw new ListenerException(e);
		} finally {
			try {
				if (mc != null) {
					mc.close();
				}
			} catch (JMSException e1) {
				throw new ListenerException("exception closing message consumer",e1);
			}
			closeSession(session);
		}
	}

	protected String getCombinedSelector(String messageId) {
		Map<String,String> selectorMap = new HashMap<>();
		selectorMap.put("JMSMessageID", messageId);
		return getCombinedSelector(selectorMap);
	}

	protected String getCombinedSelector(Map<String,String> selectors) {
		StringBuilder result = new StringBuilder();
		for (Map.Entry<String,String> entry: selectors.entrySet()) {
			if (result.length() > 0) {
				result.append(" AND ");
			}
			result.append(entry.getKey()).append("='").append(entry.getValue()).append("'");
		}

		if (StringUtils.isNotEmpty(getSelector())) {
			result.append(" AND ").append(getSelector());
		}
		return result.toString();
	}

	/**
	 * Timeout <i>in milliseconds</i> for receiving a message from the queue
	 * @deprecated use {@link #setTimeout(long)} instead
	 * @ff.default 3000
	 */
	@Deprecated(since = "8.1")
	@ConfigurationWarning("Use attribute timeout instead")
	public void setTimeOut(long newTimeOut) {
		timeout = newTimeOut;
	}

	/**
	 * Timeout <i>in milliseconds</i> for receiving a message from the queue
	 * @ff.default 3000
	 */
	public void setTimeout(long newTimeOut) {
		timeout = newTimeOut;
	}

}
