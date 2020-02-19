/*
   Copyright 2020 Integration Partners

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.containsString;


import java.io.IOException;
import java.net.URL;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.NamingException;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.jms.JmsException;
import nl.nn.adapterframework.jms.JmsMessagingSource;
import nl.nn.adapterframework.jms.MessagingSource;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.Misc;


public class IMSSenderTest extends SenderTestBase<IMSSender> {
	

	@Override
	public IMSSender createSender() {
		return new IMSSender() {
			TestJMSMessage message = (new TestJMSMessage());
			@Override
			public String getQueueConnectionFactoryName() {
				return "TESTQCF";
			}
			
			@Override
			protected MessagingSource getMessagingSource() throws JmsException {
				return mock(JmsMessagingSource.class);
			}
			
			@Override
			public Destination getDestination() throws NamingException, JMSException, JmsException {
				return null;
			}
			
			@Override
			public MessageProducer getMessageProducer(Session session, Destination destination)
					throws NamingException, JMSException {
				return mock(MessageProducer.class);
			}
			
			@Override
			protected Session createSession() throws JmsException {
				Session s = mock(Session.class);
				try {
					doReturn(message).when(s).createBytesMessage();
				} catch (JMSException e) {
					throw new JmsException(e);
				}
				return s;
			}
			
			@Override
			public MessageConsumer getMessageConsumerForCorrelationId(Session session, Destination destination,
					String correlationId) throws NamingException, JMSException {
				// TODO Auto-generated method stub
				MessageConsumer mc = mock(MessageConsumer.class);
				
				try {
					doReturn(message).when(mc).receive(getReplyTimeout());
				} catch (Exception e) {
					throw new JMSException(e.getMessage());
				}
								
				return mc;
			}
		};		
	}
	
	@Test
	public void createAndGetStringMessage() throws SenderException, TimeOutException, ConfigurationException, IOException {
		sender.setDestinationName("TEST");
		sender.setTransactionCode("UNITTEST");
		sender.setSynchronous(true);
		sender.configure();
		// sender.open(); // Do not open the sender, no MQ connections are set
		String input = "TESTMESSAGE1234%éáöî?";
		ParameterResolutionContext prc = new ParameterResolutionContext(input, session);
		String response = sender.sendMessage(null, input, prc);
		
		// For testing purposes the response BytesMessage is the same as the input BytesMessage
		// The transaction code is thus part of the response message
		assertEquals(sender.getTransactionCode() + " " + input, response);
	}
}