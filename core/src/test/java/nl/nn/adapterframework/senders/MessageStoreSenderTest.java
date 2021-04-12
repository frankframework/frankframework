package nl.nn.adapterframework.senders;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Date;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.jdbc.MessageStoreSender;
import nl.nn.adapterframework.stream.Message;

public class MessageStoreSenderTest extends SenderTestBase<MessageStoreSender> {

	@Override
	public MessageStoreSender createSender() throws Exception {
		return new MessageStoreSender() {
			@Override public void configure() { } //Suppress configure as it's will do a JNDI lookup
			@Override public void open() { } //Suppress open as it's will do a JNDI lookup

			@Override
			public String storeMessage(String messageId, String correlationId, Date receivedDate, String comments, String label, String message) throws SenderException {
				return message; //We don't actually want/need to store anything, return the input to validate the message 'to-store'
			}
		};
	}

	@Test
	public void basic() throws SenderException, TimeOutException, ConfigurationException, IOException {
		sender.configure();
		sender.open();

		String input = "<dummy/>";
		Message message = new Message(input);
		String result = sender.sendMessage(message, session).asString();
		assertEquals(input, result);
	}

	@Test
	public void withSessionKeys() throws SenderException, TimeOutException, ConfigurationException, IOException {
		session.put("sessionKey1", "value1");
		session.put("sessionKey2", new Message("value2"));
		session.put("sessionKey3", "value3".getBytes());

		sender.setSessionKeys("sessionKey1,sessionKey2,sessionKey3");
		sender.configure();
		sender.open();

		String input = "<dummy/>";
		Message message = new Message(input);
		String result = sender.sendMessage(message, session).asString();
		assertEquals(input+",value1,value2,value3", result);
	}
}
