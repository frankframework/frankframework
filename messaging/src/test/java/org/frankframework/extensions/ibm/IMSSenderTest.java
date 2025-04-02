/*
   Copyright 2020 WeAreFrank!

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.IOException;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.jms.JmsException;
import org.frankframework.jms.JmsMessagingSource;
import org.frankframework.jms.MessagingSource;
import org.frankframework.senders.SenderTestBase;
import org.frankframework.stream.Message;

public class IMSSenderTest extends SenderTestBase<IMSSender> {

	@Override
	public IMSSender createSender() {
		return new IMSSender() {
			final TestJMSMessage message = TestJMSMessage.newInstance();

			@Override
			public String getQueueConnectionFactoryName() {
				return "TESTQCF";
			}

			@Override
			public void configure() {
				// configure is not required for this test
			}

			@Override
			protected MessagingSource getMessagingSource() {
				return mock(JmsMessagingSource.class);
			}

			@Override
			public Destination getDestination() {
				return null;
			}

			@Override
			public MessageProducer getMessageProducer(Session session, Destination destination) {
				return mock(MessageProducer.class);
			}

			@Override
			protected Session createSession() throws JmsException {
				Session s = mock(Session.class);
				try {
					doAnswer(message).when(s).createBytesMessage();
				} catch (JMSException e) {
					throw new JmsException(e);
				}
				return s;
			}

			@Override
			public MessageConsumer getMessageConsumerForCorrelationId(Session session, Destination destination,
					String correlationId) throws JMSException {
				// TODO Auto-generated method stub
				MessageConsumer mc = mock(MessageConsumer.class);

				try {
					doAnswer(message).when(mc).receive(getReplyTimeout());
				} catch (Exception e) {
					throw new JMSException(e.getMessage());
				}

				return mc;
			}
		};
	}

	@Test
	public void createAndGetStringMessage() throws SenderException, TimeoutException, ConfigurationException, IOException {
		sender.setDestinationName("TEST");
		sender.setTransactionCode("UNITTEST");
		sender.setSynchronous(true);
		sender.configure();
		// sender.open(); // Do not open the sender, no MQ connections are set
		String input = "TESTMESSAGE1234%éáöî?";
		Message response = sender.sendMessageOrThrow(new Message(input), session);

		// For testing purposes the response BytesMessage is the same as the input BytesMessage
		// The transaction code is thus part of the response message
		assertEquals(sender.getTransactionCode() + " " + input, response.asString());
	}
}
