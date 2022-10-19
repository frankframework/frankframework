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
package nl.nn.adapterframework.management.bus.endpoints;

import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.Message;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.jms.JMSFacade.DestinationType;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.management.bus.ActionSelector;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusException;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.TopicSelector;
import nl.nn.adapterframework.webcontrol.api.FrankApiBase;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.QUEUE)
public class SendJmsMessage {
	private @Getter @Setter IbisManager ibisManager;

	@ActionSelector(BusAction.UPLOAD)
	public void putMessageOnQueue(Message<?> message) {
		String connectionFactory = BusMessageUtils.getHeader(message, FrankApiBase.HEADER_CONNECTION_FACTORY_NAME_KEY);
		if(StringUtils.isEmpty(connectionFactory)) {
			throw new BusException("a connectionFactory must be provided");
		}
		String destination = BusMessageUtils.getHeader(message, "destination");
		if(StringUtils.isEmpty(destination)) {
			throw new BusException("a destination must be provided");
		}
		boolean lookupDestination = BusMessageUtils.getBooleanHeader(message, "lookupDestination", false);
		boolean synchronous = BusMessageUtils.getBooleanHeader(message, "synchronous", false);
		String replyTo = BusMessageUtils.getHeader(message, "replyTo", null);
		DestinationType type = BusMessageUtils.getEnumHeader(message, "type", DestinationType.class);
		if(type == null) {
			throw new BusException("a DestinationType must be provided");
		}
		Object payload = message.getPayload();
		if(payload == null) {
			throw new BusException("no payload provided");
		}

		JmsSender qms = createJmsSender(connectionFactory, destination, type, replyTo, synchronous, lookupDestination);
		processMessage(qms, payload);
	}

	private JmsSender createJmsSender(String connectionFactory, String destination, DestinationType type, String replyTo, boolean synchronous, boolean lookupDestination) {
		JmsSender qms = getIbisManager().getIbisContext().createBeanAutowireByName(JmsSender.class);
		qms.setName("SendJmsMessageAction");
		if(type == DestinationType.QUEUE) {
			qms.setQueueConnectionFactoryName(connectionFactory);
		} else {
			qms.setTopicConnectionFactoryName(connectionFactory);
		}
		qms.setDestinationName(destination);
		qms.setDestinationType(type);
		if (StringUtils.isNotEmpty(replyTo)) {
			qms.setReplyToName(replyTo);
		}
		qms.setSynchronous(synchronous);
		qms.setLookupDestination(lookupDestination);
		return qms;
	}

	private void processMessage(JmsSender qms, Object message) {
		try {
			qms.open();
			qms.sendMessage(nl.nn.adapterframework.stream.Message.asMessage(message), null);
		} catch (Exception e) {
			throw new BusException("error occured sending message", e);
		}
		try {
			qms.close();
		} catch (Exception e) {
			throw new BusException("error occured on closing connection", e);
		}
	}
}
