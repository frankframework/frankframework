package org.frankframework.senders;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.jdbc.MessageStoreSender;
import org.frankframework.receivers.MessageWrapper;
import org.frankframework.stream.Message;

public class MessageStoreSenderInputProcessingTest extends SenderTestBase<MessageStoreSender> {

	Map<String, Serializable> mockMessageStore = new HashMap<>();

	@Override
	public MessageStoreSender createSender() throws Exception {
		return new MessageStoreSender() {
			@Override public void configure() { } // Suppress configure as it's will do a JNDI lookup
			@Override public void start() { } // Suppress start as it's will do a JNDI lookup

			@Override
			public @NotNull String storeMessage(String messageId, String correlationId, Date receivedDate, String comments, String label, Serializable message) {
				mockMessageStore.put(messageId, message);
				return messageId;
			}
		};
	}

	@Test
	public void basic() throws SenderException, TimeoutException, ConfigurationException, IOException {
		sender.configure();
		sender.start();

		String input = "<dummy/>";
		Message message = new Message(input);
		String result = sender.sendMessageOrThrow(message, session).asString();
		Serializable data = mockMessageStore.get(result);
		assertInstanceOf(Message.class, data);
		Message resultMessage = (Message) data;
		assertEquals(input, resultMessage.asString());
	}

	@Test
	public void withSessionKeys() throws SenderException, TimeoutException, ConfigurationException, IOException {
		session.put("sessionKey1", "value1");
		session.put("sessionKey2", new Message("value2"));
		session.put("sessionKey3", "value3".getBytes());

		sender.setSessionKeys("sessionKey1,sessionKey2,sessionKey3");
		sender.configure();
		sender.start();

		String input = "<dummy/>";
		Message message = new Message(input);
		String result = sender.sendMessageOrThrow(message, session).asString();
		Serializable data = mockMessageStore.get(result);
		assertInstanceOf(MessageWrapper.class, data);
		MessageWrapper<Serializable> messageWrapper = (MessageWrapper<Serializable>) data;
		Message resultMessage = messageWrapper.getMessage();
		assertEquals(input, resultMessage.asString());

		assertTrue(messageWrapper.getContext().containsKey("sessionKey1"), "Result should contain sessionKey1");
		assertTrue(messageWrapper.getContext().containsKey("sessionKey2"), "Result should contain sessionKey2");
		assertTrue(messageWrapper.getContext().containsKey("sessionKey3"), "Result should contain sessionKey3");

		assertEquals("value1", messageWrapper.getContext().get("sessionKey1"));

		assertInstanceOf(Message.class, messageWrapper.getContext().get("sessionKey2"));
		Message message2 = (Message) messageWrapper.getContext().get("sessionKey2");
		assertEquals("value2", message2.asString());

		byte[] k3 = (byte[]) messageWrapper.getContext().get("sessionKey3");
		assertArrayEquals("value3".getBytes(), k3);
	}
}
