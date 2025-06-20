package org.frankframework.larva.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeLineSession;
import org.frankframework.http.PushingListenerAdapter;
import org.frankframework.stream.Message;
import org.frankframework.util.CloseUtils;

public class TestLarvaPushingListenerAction {

	@Test
	public void testListenerMessageHandler() throws Exception {
		PushingListenerAdapter listener = new PushingListenerAdapter();
		LarvaPushingListenerAction action = new LarvaPushingListenerAction(listener, 60);

		String input = "prepare-input";
		String output = "pipeline-output";

		// before 'running' the action, prepare this 'input' message.
		Message inputMessage = new Message(input);
		action.executeWrite(inputMessage, "correlation-id-123", null);

		// execute larva

		// some background task picking up the 'input' message.
		final Message processResult;
		Message outputMessage = new Message(output);
		try (PipeLineSession session = new PipeLineSession()) {
			processResult = listener.processRequest(outputMessage, session);
		}

		assertFalse(inputMessage.isClosed()); // should not be closed after executeWrite
		assertEquals(input, processResult.asString());
		assertEquals(inputMessage, processResult); // technically (in this case) the inputMessage is the processResult.

		// validate background task result.
		Message readResult = action.executeRead(null);
		assertFalse(outputMessage.isClosed()); // should not be closed after executeRead
		assertEquals(output, readResult.asString());
		assertEquals(outputMessage, readResult); // technically (in this case) the outputMessage is the readResult.

		CloseUtils.closeSilently(action, readResult);
	}
}
