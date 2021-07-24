/*
   Copyright 2014 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.extensions.ibm;

import java.io.IOException;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicSession;
import javax.naming.NamingException;

import com.ibm.mq.jms.JMSC;
import com.ibm.mq.jms.MQDestination;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.stream.Message;

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
			Destination destination) throws NamingException, JMSException {
		setTargetClientMQ(destination);
		return super.getMessageProducer(session, destination);
	}

	@Override
	public String send(Session session, Destination dest, String correlationId, Message message, String messageType, long timeToLive, int deliveryMode, int priority, boolean ignoreInvalidDestinationException) throws NamingException, JMSException, SenderException, IOException {
		setTargetClientMQ(dest);
		return super.send(session, dest,  correlationId, message, messageType, timeToLive, deliveryMode, priority, ignoreInvalidDestinationException);
	}

	@Override
	public String send(Session session, Destination dest, javax.jms.Message message, boolean ignoreInvalidDestinationException) throws NamingException, JMSException {
		setTargetClientMQ(dest);
		return super.send(session, dest, message, ignoreInvalidDestinationException);
	}

	@Override
	protected String sendByQueue(QueueSession session, Queue destination,
			javax.jms.Message message) throws NamingException, JMSException {
		setTargetClientMQ(destination);
		return super.sendByQueue(session, destination, message);
	}

	@Override
	protected String sendByTopic(TopicSession session, Topic destination,
			javax.jms.Message message) throws NamingException, JMSException {
		setTargetClientMQ(destination);
		return super.sendByTopic(session, destination, message);
	}

	private void setTargetClientMQ(Destination destination) throws JMSException {
		log.debug("[" + getName() + "] set target client for queue [" + destination.toString() + "] to NONJMS_MQ");
		((MQDestination)destination).setTargetClient(JMSC.MQJMS_CLIENT_NONJMS_MQ);
	}

}
