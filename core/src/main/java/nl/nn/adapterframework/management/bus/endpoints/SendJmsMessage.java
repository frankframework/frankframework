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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.jms.JMSFacade.DestinationType;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.management.bus.ActionSelector;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusException;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.ResponseMessage;
import nl.nn.adapterframework.management.bus.TopicSelector;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.util.LogUtil;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.QUEUE)
public class SendJmsMessage extends BusEndpointBase {

	@ActionSelector(BusAction.UPLOAD)
	public Message<Object> putMessageOnQueue(Message<?> message) {
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
		String replyTo = BusMessageUtils.getHeader(message, "replyTo", null);
		String messageProperty = BusMessageUtils.getHeader(message, "messageProperty", null);
		DestinationType type = BusMessageUtils.getEnumHeader(message, "type", DestinationType.class);
		if(type == null) {
			throw new BusException("a DestinationType must be provided");
		}

		JmsSender qms = createJmsSender(connectionFactory, destination, persistent, type, replyTo, expectsReply, lookupDestination);
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

	private JmsSender createJmsSender(String connectionFactory, String destination, boolean persistent, DestinationType type, String replyTo, boolean synchronous, boolean lookupDestination) {
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
		return qms;
	}

	private Message<Object> processMessage(JmsSender qms, Object requestMessage, boolean expectsReply) {
		try {
			qms.open();
			nl.nn.adapterframework.stream.Message responseMessage = qms.sendMessageOrThrow(nl.nn.adapterframework.stream.Message.asMessage(requestMessage), null);
			return expectsReply ? ResponseMessage.Builder.create().withPayload(responseMessage).raw() : null;
		} catch (Exception e) {
			throw new BusException("error occured sending message", e);
		} finally {
			try {
				qms.close();
			} catch (Exception e) {
				LogUtil.getLogger(this).error("unable to close connection", e);
			}
		}
	}
}
