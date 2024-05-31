package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeLineSession;

class RestoreMovedElementsHandlerTest {

	@Test
	void testNoInputRestoreFlow() {
		String inputString = ""; // validate empty input
		PipeLineSession pipeLineSession = new PipeLineSession();
		pipeLineSession.put("1", "123");
		String result = RestoreMovedElementsHandler.process(inputString, pipeLineSession);
		assertEquals("", result);
		assertEquals("123", pipeLineSession.get("1"));

		assertNull(RestoreMovedElementsHandler.process(null, pipeLineSession)); // validate null input
		assertEquals(inputString, RestoreMovedElementsHandler.process(inputString, null)); // without session, return input
	}

	@Test
	void testBasicRestoreFlow() {
		String inputString = "{sessionKey:1}abc{sessionKey:2}def{sessionKey:3}";
		PipeLineSession pipeLineSession = new PipeLineSession();
		pipeLineSession.put("1", "123");
		pipeLineSession.put("2", "456");
		pipeLineSession.put("3", "789");
		String result = RestoreMovedElementsHandler.process(inputString, pipeLineSession);
		assertEquals("123abc456def789", result);
	}

	@Test
	void testCorruptedInputFlow1() {
		String inputString = "xxxxxxx{session Key:1}abc{sessionKeydef{sessionKey:3{sessionKey:2}{sessionKey:1}}}}}}{sessionKey:notThere}";
		PipeLineSession pipeLineSession = new PipeLineSession();
		pipeLineSession.put("1", "123");
		pipeLineSession.put("2", "456");
		pipeLineSession.put("3", "789");
		pipeLineSession.put("33", "123");
		pipeLineSession.put("not There", "123");
		String result = RestoreMovedElementsHandler.process(inputString, pipeLineSession);
		assertEquals("xxxxxxx{session Key:1}abc{sessionKeydef{sessionKey:3456123}}}}}{sessionKey:notThere}", result);
	}

	@Test
	void testCorruptedInputFlow2() {
		String inputString = "{sessionKey:{sessionKey:{sessionKey:{sessionKey:}{{{{{{}}}{{{sessionKey:";
		PipeLineSession pipeLineSession = new PipeLineSession();
		String result = RestoreMovedElementsHandler.process(inputString, pipeLineSession);
		assertEquals("{sessionKey:{sessionKey:{sessionKey:{sessionKey:}{{{{{{}}}{{{sessionKey:", result);
	}

	@Test
	void testCorruptedInputFlow3() {
		String inputString = "{sessionKey: $$$$%%§§§§§§§§§§``````````%% }";
		PipeLineSession pipeLineSession = new PipeLineSession();
		pipeLineSession.put(" $$$$%%", Integer.MAX_VALUE);
		String result = RestoreMovedElementsHandler.process(inputString, pipeLineSession);
		assertEquals("{sessionKey: $$$$%%§§§§§§§§§§``````````%% }", result);
	}

	@Test
	void testCorruptedInputFlow4() {
		String inputString = "{sessionKey:1}abc{sessionKey:2}def{sessionKey:3}";
		PipeLineSession pipeLineSession = new PipeLineSession();
		pipeLineSession.put("1", "{sessionKey:2}");
		pipeLineSession.put("-2", "{sessionKey:3}");
		pipeLineSession.put("abc", "efg");
		String result = RestoreMovedElementsHandler.process(inputString, pipeLineSession);
		assertEquals("{sessionKey:2}abc{sessionKey:2}def{sessionKey:3}", result);
	}

}
