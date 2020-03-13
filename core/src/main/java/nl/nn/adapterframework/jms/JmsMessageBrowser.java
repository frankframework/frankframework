/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.jms;

import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.Misc;

/**
 * Get the messages on a queue without deleting them
 * @author  Johan Verrips / Gerrit van Brakel
 * @see nl.nn.adapterframework.webcontrol.action.BrowseQueue
 */
public class JmsMessageBrowser extends JMSFacade implements IMessageBrowser {

	private long timeOut = 3000;
	private String selector=null;

	private String hideRegex = null;
	private String hideMethod = "all";
	
	
	public JmsMessageBrowser() {
		super();
		setTransacted(true);
	}

	public JmsMessageBrowser(String selector) {
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
			selector=Misc.concatStrings(selector, " AND ", "JMSTimestamp >= "+DateUtils.format(startTime));
		}
		if (endTime!=null) {
			selector=Misc.concatStrings(selector, " AND ", "JMSTimestamp < "+DateUtils.format(endTime));
		}
		try {
			return new JmsQueueBrowserIterator(this,(Queue)getDestination(),selector);
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}

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
			for (Enumeration enm=queueBrowser.getEnumeration();enm.hasMoreElements();enm.nextElement()) {
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

	
	@Override
	public Object getMessage(String messageId) throws ListenerException {
		Session session=null;
		Object msg = null;
		MessageConsumer mc = null;
		try {
			session = createSession();
			mc = getMessageConsumer(session, getDestination(), getCombinedSelector(messageId));
			msg = mc.receive(getTimeOut());
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

	@Override
	public Object browseMessage(String messageId) throws ListenerException {
		return doBrowse("JMSMessageID", messageId);
	}


	protected Message doBrowse(Map<String,String> selectors) throws ListenerException {
		QueueSession session=null;
		Message msg = null;
		QueueBrowser queueBrowser=null;
		try {
			session = (QueueSession)createSession();
			queueBrowser = session.createBrowser((Queue)getDestination(),getCombinedSelector(selectors));
			Enumeration msgenum = queueBrowser.getEnumeration();
			if (msgenum.hasMoreElements()) {
				msg=(Message)msgenum.nextElement();
			}
			return msg;
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

	protected Message doBrowse(String selectorKey, String selectorValue) throws ListenerException {
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
			log.debug("retrieving message ["+messageId+"] in order to delete it");
			mc = getMessageConsumer(session, getDestination(), getCombinedSelector(messageId));
			mc.receive(getTimeOut());
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
		StringBuffer result = new StringBuffer();
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

	public void setTimeOut(long newTimeOut) {
		timeOut = newTimeOut;
	}
	public long getTimeOut() {
		return timeOut;
	}


	public String getSelector() {
		return selector;
	}

	@Override
	@IbisDoc({"Regular expression to mask strings in the errorStore/logStore. Every character between to the strings in this expression will be replaced by a '*'. For example, the regular expression (?&lt;=&lt;party&gt;).*?(?=&lt;/party&gt;) will replace every character between keys<party> and </party> ", ""})
	public void setHideRegex(String hideRegex) {
		this.hideRegex = hideRegex;
	}
	@Override
	public String getHideRegex() {
		return hideRegex;
	}

	@Override
	@IbisDoc({"(Only used when hideRegex is not empty) either <code>all</code> or <code>firstHalf</code>. When <code>firstHalf</code> only the first half of the string is masked, otherwise (<code>all</code>) the entire string is masked", "all"})
	public void setHideMethod(String hideMethod) {
		this.hideMethod = hideMethod;
	}
	@Override
	public String getHideMethod() {
		return hideMethod;
	}

}

