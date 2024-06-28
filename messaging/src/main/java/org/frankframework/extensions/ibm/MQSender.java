/*
   Copyright 2014 Nationale-Nederlanden, 2020, 2024 WeAreFrank!

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
package org.frankframework.extensions.ibm;

import java.io.IOException;

import org.frankframework.core.SenderException;
import org.frankframework.jms.JmsSender;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import jakarta.jms.TopicSession;

/**
 * JMS sender which will call IBM WebSphere MQ specific
 * setTargetClient(JMSC.MQJMS_CLIENT_NONJMS_MQ) on the destination prior to
 * sending a message. This is needed when the MQ destination is not a JMS
 * receiver otherwise format errors occur (e.g. dots are added after every
 * character in the message).
 *
 * <p>See {@link JmsSender} for configuration</p>
 *
 * @author Jaco de Groot
 */

public class MQSender extends JmsSender {

	@Override
	public MessageProducer getMessageProducer(Session session,
			Destination destination) throws JMSException {
		setTargetClientMQ(destination);
		return super.getMessageProducer(session, destination);
	}

	@Override
	public String send(Session session, Destination dest, String correlationId, Message message, String messageType, long timeToLive, int deliveryMode, int priority, boolean ignoreInvalidDestinationException) throws JMSException, SenderException, IOException {
		setTargetClientMQ(dest);
		return super.send(session, dest,  correlationId, message, messageType, timeToLive, deliveryMode, priority, ignoreInvalidDestinationException);
	}

	@Override
	public String send(Session session, Destination dest, jakarta.jms.Message message, boolean ignoreInvalidDestinationException) throws JMSException {
		setTargetClientMQ(dest);
		return super.send(session, dest, message, ignoreInvalidDestinationException);
	}

	@Override
	protected String sendByQueue(QueueSession session, Queue destination,
			jakarta.jms.Message message) throws JMSException {
		setTargetClientMQ(destination);
		return super.sendByQueue(session, destination, message);
	}

	@Override
	protected String sendByTopic(TopicSession session, Topic destination,
			jakarta.jms.Message message) throws JMSException {
		setTargetClientMQ(destination);
		return super.sendByTopic(session, destination, message);
	}

	private void setTargetClientMQ(Destination destination) throws JMSException {
		log.debug("[{}] set target client for queue [{}] to NONJMS_MQ", getName(), destination.toString());
		try {
			ClassUtils.invokeSetter(destination, "setTargetClient", 1); //JMSC.MQJMS_CLIENT_NONJMS_MQ
		} catch (Exception e) {
			throw new JMSException("unable to set TargetClient", "0", e);
		}
	}

}
