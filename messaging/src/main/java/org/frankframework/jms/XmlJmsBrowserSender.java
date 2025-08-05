/*
   Copyright 2013 Nationale-Nederlanden, 2021-2025 WeAreFrank!

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

import jakarta.annotation.Nonnull;

import org.w3c.dom.Element;

import org.frankframework.core.IMessageBrowsingIterator;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.jms.JMSFacade.JmsDestinationType;
import org.frankframework.senders.AbstractSenderWithParameters;
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
 * <pre>{@code
 * <browse>
 *    <jmsRealm>qcf</jmsRealm>
 *    <destinationName>jms/GetPolicyDetailsRequest</destinationName>
 *    <destinationType>QUEUE</destinationType>
 * </browse>
 * }</pre>
 * </p>
 * <p>
 * <b>example (browse output):</b>
 * <pre>{@code
 * <result>
 *   <items count="2">
 *      <item>
 *         <timestamp>Thu Nov 20 13:36:31 CET 2014</timestamp>
 *         <messageId>ID:LPAB00000003980-61959-1416486781822-3:5:33:1:1</messageId>
 *         <correlationId>...</correlationId>
 *         <message><![CDATA[...]]></message>
 *      </item>
 *      <item>
 *         <timestamp>Thu Dec 12 11:59:22 CET 2014</timestamp>
 *         <messageId>ID:LPAB00000003980-58359-1721486799722-3:4:19:1:1</messageId>
 *         <correlationId>...</correlationId>
 *         <message><![CDATA[...]]></message>
 *      </item>
 * 	 </items>
 * </result>
 * }</pre>
 * </p>
 *
 * <p>
 * <b>example (remove output):</b>
 * <pre>{@code
 * <result>
 *     <itemsRemoved>2</itemsRemoved>
 * </result>
 * }</pre>
 *
 * @author  Peter Leeuwenburgh
 */
public class XmlJmsBrowserSender extends AbstractSenderWithParameters {

	@SuppressWarnings("unchecked")
	public JmsBrowser<jakarta.jms.Message> createJmsBrowser() {
		return createBean(JmsBrowser.class);
	}

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		Element queueBrowserElement;
		String root = null;
		String jmsRealm = null;
		String queueConnectionFactoryName = null;
		String destinationName = null;
		JmsDestinationType destinationType = null;
		try {
			queueBrowserElement = XmlUtils.buildElement(message.asString());
			root = queueBrowserElement.getTagName();
			jmsRealm = XmlUtils.getChildTagAsString(queueBrowserElement, "jmsRealm");
			queueConnectionFactoryName = XmlUtils.getChildTagAsString(queueBrowserElement, "queueConnectionFactoryName");
			destinationName = XmlUtils.getChildTagAsString(queueBrowserElement, "destinationName");
			destinationType = EnumUtils.parse(JmsDestinationType.class,XmlUtils.getChildTagAsString(queueBrowserElement, "destinationType"));
		} catch (Exception e) {
			throw new SenderException("got exception parsing [" + message + "]", e);
		}

		JmsBrowser<jakarta.jms.Message> jmsBrowser = createJmsBrowser();
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

		if ("browse".equalsIgnoreCase(root)) {
			// OK
		} else {
			if ("remove".equalsIgnoreCase(root)) {
				remove = true;
			} else {
				throw new SenderException("unknown root element [" + root + "]");
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
			throw new SenderException("got exception browsing messages", e);
		} finally {
			try {
				if (it != null) {
					it.close();
				}
			} catch (ListenerException e) {
				log.warn("exception on closing message browser iterator", e);
			}
		}
		return new SenderResult(result.asMessage());
	}
}
