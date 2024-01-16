/*
   Copyright 2013 Nationale-Nederlanden, 2021, 2022 WeAreFrank!

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

import org.w3c.dom.Element;

import org.frankframework.core.IMessageBrowsingIterator;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.jms.JMSFacade.DestinationType;
import org.frankframework.senders.SenderWithParametersBase;
import org.frankframework.stream.Message;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.XmlBuilder;
import org.frankframework.util.XmlUtils;

/**
 * Sender for browsing and removing queue messages (with input and output in a XML message).
 *
 * <p>
 * When input root element is <code>browse</code> all queue messages are returned.
 * </p>
 * <p>
 * When input root element is <code>remove</code> all queue messages are removed.
 * </p>
 * <p>
 * <b>example (input):</b>
 * <code>
 * <pre>
 *   &lt;browse&gt;
 *      &lt;jmsRealm&gt;qcf&lt;/jmsRealm&gt;
 *      &lt;destinationName&gt;jms/GetPolicyDetailsRequest&lt;/destinationName&gt;
 *      &lt;destinationType&gt;QUEUE&lt;/destinationType&gt;
 *   &lt;/browse>
 * </pre>
 * </code>
 * </p>
 *
 *
 * <p>
 * <b>example (browse output):</b>
 * <code>
 * <pre>
 *   &lt;result&gt;
 *	    &lt;items count="2"&gt;
 *	       &lt;item&gt;
 *	          &lt;timestamp&gt;Thu Nov 20 13:36:31 CET 2014&lt;/timestamp&gt;
 *	          &lt;messageId&gt;ID:LPAB00000003980-61959-1416486781822-3:5:33:1:1&lt;/messageId&gt;
 *	          &lt;correlationId&gt;...&lt;/correlationId&gt;
 *	          &lt;message&gt;&lt;![CDATA[...]]&gt;&lt;/message&gt;
 *	       &lt;/item&gt;
 *	       &lt;item&gt;
 *	          &lt;timestamp&gt;Thu Dec 12 11:59:22 CET 2014&lt;/timestamp&gt;
 *	          &lt;messageId&gt;ID:LPAB00000003980-58359-1721486799722-3:4:19:1:1&lt;/messageId&gt;
 *	          &lt;correlationId&gt;...&lt;/correlationId&gt;
 *	          &lt;message&gt;&lt;![CDATA[...]]&gt;&lt;/message&gt;
 *	       &lt;/item&gt;
 *	    &lt;/items&gt;
 *   &lt;/result&gt;
 * </pre>
 * </code>
 * </p>
 *
 * <p>
 * <b>example (remove output):</b>
 * <code>
 * <pre>
 *   &lt;result&gt;
 *	    &lt;itemsRemoved&gt;2&lt;/itemsRemoved&gt;
 *   &lt;/result&gt;
 * </pre>
 * </code>
 * </p>
 *
 * @author  Peter Leeuwenburgh
 */
public class XmlJmsBrowserSender extends SenderWithParametersBase {

	@SuppressWarnings("unchecked")
	public JmsBrowser<javax.jms.Message> createJmsBrowser() {
		return createBean(JmsBrowser.class);
	}

	@Override
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		Element queueBrowserElement;
		String root = null;
		String jmsRealm = null;
		String queueConnectionFactoryName = null;
		String destinationName = null;
		DestinationType destinationType = null;
		try {
			queueBrowserElement = XmlUtils.buildElement(message.asString());
			root = queueBrowserElement.getTagName();
			jmsRealm = XmlUtils.getChildTagAsString(queueBrowserElement, "jmsRealm");
			queueConnectionFactoryName = XmlUtils.getChildTagAsString(queueBrowserElement, "queueConnectionFactoryName");
			destinationName = XmlUtils.getChildTagAsString(queueBrowserElement, "destinationName");
			destinationType = EnumUtils.parse(DestinationType.class,XmlUtils.getChildTagAsString(queueBrowserElement, "destinationType"));
		} catch (Exception e) {
			throw new SenderException(getLogPrefix() + "got exception parsing [" + message + "]", e);
		}

		JmsBrowser<javax.jms.Message> jmsBrowser = createJmsBrowser();
		jmsBrowser.setName("XmlQueueBrowserSender");
		if (jmsRealm != null) {
			jmsBrowser.setJmsRealm(jmsRealm);
		}
		if (queueConnectionFactoryName != null) {
			jmsBrowser.setQueueConnectionFactoryName(queueConnectionFactoryName);
		}
		jmsBrowser.setDestinationName(destinationName);
		jmsBrowser.setDestinationType(destinationType);
		IMessageBrowsingIterator it = null;

		boolean remove = false;

		if (root.equalsIgnoreCase("browse")) {
			// OK
		} else {
			if (root.equalsIgnoreCase("remove")) {
				remove = true;
			} else {
				throw new SenderException(getLogPrefix()
						+ "unknown root element [" + root + "]");
			}
		}

		XmlBuilder result = new XmlBuilder("result");
		XmlBuilder items;
		if (remove) {
			items = new XmlBuilder("itemsRemoved");
		} else {
			items = new XmlBuilder("items");
		}
		try {
			int count = 0;
			it = jmsBrowser.getIterator();
			while (it.hasNext()) {
				count++;
				JmsMessageBrowserIteratorItem jmsMessageBrowserIteratorItem = (JmsMessageBrowserIteratorItem) it
						.next();
				if (remove) {
					jmsBrowser.deleteMessage(jmsMessageBrowserIteratorItem
							.getJMSMessageID());
				} else {
					// browse
					XmlBuilder item = new XmlBuilder("item");
					XmlBuilder timestamp = new XmlBuilder("timestamp");
					timestamp.setValue(new java.util.Date(
							jmsMessageBrowserIteratorItem.getJMSTimestamp())
							.toString());
					item.addSubElement(timestamp);
					XmlBuilder messageId = new XmlBuilder("messageId");
					messageId.setValue(jmsMessageBrowserIteratorItem
							.getJMSMessageID());
					item.addSubElement(messageId);
					XmlBuilder correlationId = new XmlBuilder("correlationId");
					correlationId.setValue(jmsMessageBrowserIteratorItem
							.getCorrelationId());
					item.addSubElement(correlationId);
					XmlBuilder msg = new XmlBuilder("message");
					msg.setCdataValue(jmsMessageBrowserIteratorItem.getText());
					item.addSubElement(msg);
					items.addSubElement(item);
				}
			}
			if (remove) {
				items.setValue(Integer.toString(count));
			} else {
				items.addAttribute("count", count);
			}
			result.addSubElement(items);
		} catch (ListenerException e) {
			throw new SenderException(getLogPrefix()
					+ "got exception browsing messages", e);
		} finally {
			try {
				if (it != null) {
					it.close();
				}
			} catch (ListenerException e) {
				log.warn(getLogPrefix()
						+ "exception on closing message browser iterator", e);
			}
		}
		return new SenderResult(result.toXML());
	}
}
