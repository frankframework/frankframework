/*
   Copyright 2022 WeAreFrank!

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
package org.frankframework.management.bus.endpoints;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.security.RolesAllowed;

import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.Message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.core.IMessageBrowsingIterator;
import org.frankframework.core.IMessageBrowsingIteratorItem;
import org.frankframework.core.ListenerException;
import org.frankframework.jms.IConnectionFactoryFactory;
import org.frankframework.jms.JMSFacade.DestinationType;
import org.frankframework.jms.JmsBrowser;
import org.frankframework.jms.JmsMessageBrowserIteratorItem;
import org.frankframework.jms.JmsRealmFactory;
import org.frankframework.management.bus.ActionSelector;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.management.bus.message.JsonMessage;

@Log4j2
@BusAware("frank-management-bus")
@TopicSelector(BusTopic.QUEUE)
public class BrowseQueue extends BusEndpointBase {

	@ActionSelector(BusAction.GET)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<String> getConnectionFactories(Message<?> message) {
		Map<String, Object> returnMap = new HashMap<>();
		IConnectionFactoryFactory connectionFactoryFactory = getBean("connectionFactoryFactory", IConnectionFactoryFactory.class);
		Set<String> connectionFactories = new LinkedHashSet<>();
		// connection factories used in configured jmsSenders etc.
		connectionFactories.addAll(connectionFactoryFactory.getConnectionFactoryNames());
		// configured jms realm
		connectionFactories.addAll(JmsRealmFactory.getInstance().getConnectionFactoryNames());
		if (connectionFactories.isEmpty()) connectionFactories.add("no connection factories found");
		returnMap.put("connectionFactories", connectionFactories);

		return new JsonMessage(returnMap);
	}

	@ActionSelector(BusAction.FIND)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<String> findMessagesOnQueue(Message<?> message) {
		String connectionFactory = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONNECTION_FACTORY_NAME_KEY);
		if(StringUtils.isEmpty(connectionFactory)) {
			throw new BusException("a connectionFactory must be provided");
		}
		String destination = BusMessageUtils.getHeader(message, "destination");
		boolean lookupDestination = BusMessageUtils.getBooleanHeader(message, "lookupDestination", false);
		boolean showPayload = BusMessageUtils.getBooleanHeader(message, "showPayload", false);
		boolean rowNumbersOnly = BusMessageUtils.getBooleanHeader(message, "rowNumbersOnly", false);
		DestinationType type = BusMessageUtils.getEnumHeader(message, "type", DestinationType.class);
		if(type == null) {
			throw new BusException("a DestinationType must be provided");
		}

		Map<String, Object> returnMap = new HashMap<>();

		try {
			@SuppressWarnings("unchecked")
			JmsBrowser<jakarta.jms.Message> jmsBrowser = createBean(JmsBrowser.class);
			jmsBrowser.setName("BrowseQueueAction");
			if(type == DestinationType.QUEUE) {
				jmsBrowser.setQueueConnectionFactoryName(connectionFactory);
			} else {
				jmsBrowser.setTopicConnectionFactoryName(connectionFactory);
			}
			jmsBrowser.setDestinationName(destination);
			jmsBrowser.setDestinationType(type);
			jmsBrowser.setLookupDestination(lookupDestination);

			List<MessageBrowsingItemDOA> messages = new ArrayList<>();
			try (IMessageBrowsingIterator it = jmsBrowser.getIterator()) {
				while (it.hasNext()) {
					messages.add(new MessageBrowsingItemDOA(it.next(), showPayload));
				}
			}

			log.debug("Browser returned [{}] messages", messages::size);
			returnMap.put("numberOfMessages", messages.size());

			if(!rowNumbersOnly) {
				returnMap.put("messages", messages);
			}

			return new JsonMessage(returnMap);
		}
		catch (Exception e) {
			throw new BusException("Error occurred browsing messages", e);
		}
	}

	private class MessageBrowsingItemDOA {
		private @Getter Date expiryDate;
		private final @Getter String host;
		private @Getter Date insertDate;
		private final @Getter String comment;
		private final @Getter String correlationId;
		private final @Getter String id;
		private @Getter @JsonInclude(Include.NON_NULL) String text;

		public MessageBrowsingItemDOA(IMessageBrowsingIteratorItem item, boolean showPayload) throws ListenerException {
			comment = item.getCommentString();
			correlationId = item.getCorrelationId();
			try {
				expiryDate = item.getExpiryDate();
			} catch (Exception e) {
				log.warn("Could not get expiryDate", e);
			}
			host = item.getHost();
			id = item.getId();
			try {
				insertDate = item.getInsertDate();
			} catch (Exception e) {
				log.warn("Could not get insertDate", e);
			}
			if(showPayload && item instanceof JmsMessageBrowserIteratorItem iteratorItem) {
				text = iteratorItem.getText();
			}
		}
	}
}
