package org.frankframework.larva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeLineSession;
import org.frankframework.http.PushingListenerAdapter;
import org.frankframework.stream.Message;

public class TestListenerMessageHandler {

	@Test
	public void testListenerMessageHandler() throws Exception {
		ListenerMessageHandler<Message> handler = new ListenerMessageHandler<>(60);
		PipeLineSession session = new PipeLineSession();
		handler.putResponseMessage(new ListenerMessage(new Message("fixed response"), session));

		PushingListenerAdapter listener = new PushingListenerAdapter();
		listener.setHandler(handler);

		try (PipeLineSession newSession = new PipeLineSession()) {
			Message input = new Message("process me!");
			Message result = listener.processRequest(input, newSession);
			assertEquals("fixed response", result.asString());
		}

		ListenerMessage request = handler.getRequestMessageOrNull();
		assertNotNull(request);
		assertEquals("process me!", request.getMessage().asString());

		session.close();
	}
}
