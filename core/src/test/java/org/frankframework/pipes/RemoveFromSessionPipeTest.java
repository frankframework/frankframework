package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeRunResult;

/**
 * RemoveFromSessionPipe Tester.
 *
 * @author <Sina Sen>
 */
public class RemoveFromSessionPipeTest extends PipeTestBase<RemoveFromSessionPipe> {

	@Override
	public RemoveFromSessionPipe createPipe() {
		return new RemoveFromSessionPipe();
	}

	@Test
	public void testEmptySessionKeyNonEmptyInput() throws Exception {
		pipe.setSessionKey(null);
		session.put("a", "123");
		PipeRunResult res = doPipe(pipe, "a", session);
		assertEquals("123", res.getResult().asString());
	}

	@Test
	public void testNonEmptySessionKeyNonEmptyInput() throws Exception {
		pipe.setSessionKey("a");
		session.put("a", "123");
		PipeRunResult res = doPipe(pipe, "a", session);
		assertEquals("123", res.getResult().asString());
	}

	@Test
	public void testNonEmptySessionKeyEmptyInput() throws Exception {
		pipe.setSessionKey("a");
		session.put("a", "123");
		PipeRunResult res = pipe.doPipe(null, session);
		assertEquals("123", res.getResult().asString());
	}

	@Test
	public void testNullInput() {
		pipe.setSessionKey("");
		session.put("a", "123");

		assertThrows(NullPointerException.class, () -> doPipe(pipe, null, session));
	}

	@Test
	public void testFailAsKeyIsWrong() throws Exception {
		pipe.setSessionKey("ab");
		session.put("a", "123");
		PipeRunResult res = doPipe(pipe, "ab", session);
		assertEquals("[null]", res.getResult().asString());
	}

}
