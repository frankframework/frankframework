/*
   Copyright 2022-2023 WeAreFrank!

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

import jakarta.annotation.security.RolesAllowed;

import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.Message;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.jms.JMSFacade;
import org.frankframework.jms.JMSFacade.DestinationType;
import org.frankframework.jms.JmsSender;
import org.frankframework.management.bus.ActionSelector;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.management.bus.message.BinaryMessage;
import org.frankframework.management.bus.message.StringMessage;
import org.frankframework.parameters.Parameter;
import org.frankframework.util.LogUtil;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.QUEUE)
public class SendJmsMessage extends BusEndpointBase {

	@ActionSelector(BusAction.UPLOAD)
	@RolesAllowed("IbisTester")
	public Message<?> putMessageOnQueue(Message<?> message) {
		String connectionFactory = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONNECTION_FACTORY_NAME_KEY);
		if(StringUtils.isEmpty(connectionFactory)) {
			throw new BusException("a connectionFactory must be provided");
		}
		String destination = BusMessageUtils.getHeader(message, "destination");
		if(StringUtils.isEmpty(destination)) {
			throw new BusException("a destination must be provided");
		}
		boolean lookupDestination = BusMessageUtils.getBooleanHeader(message, "lookupDestination", false);
		boolean expectsReply = message.getHeaders().containsKey("replyChannel");
		boolean persistent = message.getHeaders().containsKey("persistent");
		JMSFacade.MessageClass messageClass = BusMessageUtils.getEnumHeader(message, "messageClass", JMSFacade.MessageClass.class, JMSFacade.MessageClass.AUTO);
		String replyTo = BusMessageUtils.getHeader(message, "replyTo", null);
		String messageProperty = BusMessageUtils.getHeader(message, "messageProperty", null);
		DestinationType type = BusMessageUtils.getEnumHeader(message, "type", DestinationType.class);
		if(type == null) {
			throw new BusException("a DestinationType must be provided");
		}

		JmsSender qms = createJmsSender(connectionFactory, destination, persistent, type, replyTo, expectsReply, lookupDestination, messageClass);
		if(StringUtils.isNotEmpty(messageProperty)) {
			qms.addParameter(getMessagePropertyParameter(messageProperty));
		}
		return processMessage(qms, message.getPayload(), expectsReply);
	}

	private Parameter getMessagePropertyParameter(String messageProperty) {
		String[] keypair = messageProperty.split(",");
		Parameter p = createBean(Parameter.class);
		p.setName(keypair[0]);
		p.setValue(keypair[1]);
		try {
			p.configure();
		} catch (ConfigurationException e) {
			throw new BusException("Failed to configure message property ["+p.getName()+"]", e);
		}
		return p;
	}

	private JmsSender createJmsSender(String connectionFactory, String destination, boolean persistent, DestinationType type, String replyTo, boolean synchronous, boolean lookupDestination, JMSFacade.MessageClass messageClass) {
		JmsSender qms = createBean(JmsSender.class);
		qms.setName("SendJmsMessageAction");
		if(type == DestinationType.QUEUE) {
			qms.setQueueConnectionFactoryName(connectionFactory);
		} else {
			qms.setTopicConnectionFactoryName(connectionFactory);
		}
		qms.setDestinationName(destination);
		qms.setDestinationType(type);
		qms.setPersistent(persistent);
		if (StringUtils.isNotEmpty(replyTo)) {
			qms.setReplyToName(replyTo);
		}
		qms.setSynchronous(synchronous);
		qms.setLookupDestination(lookupDestination);
		qms.setMessageClass(messageClass);
		return qms;
	}

	private Message<?> processMessage(JmsSender qms, Object requestMessage, boolean expectsReply) {
		try(PipeLineSession session = new PipeLineSession()) {
			qms.start();
			org.frankframework.stream.Message responseMessage = qms.sendMessageOrThrow(org.frankframework.stream.Message.asMessage(requestMessage), session);
			if(!expectsReply) {
				return null;
			}

			if(responseMessage.isBinary()) {
				return new BinaryMessage(responseMessage.asInputStream());
			}
			return new StringMessage(responseMessage.asString());
		} catch (Exception e) {
			throw new BusException("error occurred sending message", e);
		} finally {
			try {
				qms.stop();
			} catch (Exception e) {
				LogUtil.getLogger(this).error("unable to close connection", e);
			}
		}
	}
}
