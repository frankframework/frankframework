package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.Parameter;
import org.frankframework.stream.Message;

public class FixedForwardPipeTest extends PipeTestBase<FixedForwardPipe> {

	@Override
	public FixedForwardPipe createPipe() throws ConfigurationException {
		return new FixedForwardPipe() {
			@Override
			public PipeRunResult doPipe(Message message, PipeLineSession session) {
				return null;
			}
		};
	}

	@Test
	public void testPlain() throws Exception {
		configureAndStartPipe();
		assertFalse(pipe.skipPipe(null, session));
	}

	@Test
	public void testSkipOnEmptyInput() throws Exception {
		pipe.setSkipOnEmptyInput(true);
		configureAndStartPipe();
		assertTrue(pipe.skipPipe(null, session));
		assertTrue(pipe.skipPipe(new Message(""), session));
		assertTrue(pipe.skipPipe(new Message((String)null), session));
		assertFalse(pipe.skipPipe(new Message("a"), session));
	}

	public void testSkipOnOnlyIfSessionKey(String onlyIfValue, String sessionKeyValue, boolean expected) throws Exception {
		pipe.setOnlyIfSessionKey("onlyIfSessionKey");
		pipe.setOnlyIfValue(onlyIfValue);
		configureAndStartPipe();
		if (sessionKeyValue!=null) {
			if ("null".equals(sessionKeyValue)) {
				sessionKeyValue=null;
			}
			session.put("onlyIfSessionKey", sessionKeyValue);
		}
		assertEquals(expected, pipe.skipPipe(null, session));
	}

	@Test
	public void testOnlyIfSessionKey() throws Exception {
		testSkipOnOnlyIfSessionKey("a", "a", false);
	}

	@Test
	public void testOnlyIfSessionKeyNoMatch() throws Exception {
		testSkipOnOnlyIfSessionKey("a", "b", true);
	}

	@Test
	public void testOnlyIfSessionKeyNoSessionKey() throws Exception {
		testSkipOnOnlyIfSessionKey("a", null, true);
	}

	@Test
	public void testOnlyIfSessionKeyNullValue() throws Exception {
		testSkipOnOnlyIfSessionKey("a", "null", true);
	}

	@Test
	public void testOnlyIfSessionKeyNoIfValueNoSessionKey() throws Exception {
		testSkipOnOnlyIfSessionKey(null, null, true);
	}

	@Test
	public void testOnlyIfSessionKeyNoIfValueNullValue() throws Exception {
		testSkipOnOnlyIfSessionKey(null, "null", true);
	}

	@Test
	public void testOnlyIfSessionKeyNoIfValue2() throws Exception {
		testSkipOnOnlyIfSessionKey(null, "a", false);
	}


	public void testSkipOnUnlessSessionKey(String unlessValue, String sessionKeyValue, boolean expected) throws Exception {
		pipe.setUnlessSessionKey("unlessSessionKey");
		pipe.setUnlessValue(unlessValue);
		configureAndStartPipe();
		if (sessionKeyValue!=null) {
			if ("null".equals(sessionKeyValue)) {
				sessionKeyValue=null;
			}
			session.put("unlessSessionKey", sessionKeyValue);
		}
		assertEquals(expected, pipe.skipPipe(null, session));
	}

	@Test
	public void testUnlessSessionKey() throws Exception {
		testSkipOnUnlessSessionKey("a", "a", true);
	}

	@Test
	public void testUnlessSessionKeyNoMatch() throws Exception {
		testSkipOnUnlessSessionKey("a", "b", false);
	}

	@Test
	public void testUnlessSessionKeyNoSessionKey() throws Exception {
		testSkipOnUnlessSessionKey("a", null, false);
	}

	@Test
	public void testUnlessSessionKeyNullValue() throws Exception {
		testSkipOnUnlessSessionKey("a", "null", false);
	}

	@Test
	public void testUnlessSessionKeyNoIfValueNoSessionKey() throws Exception {
		testSkipOnUnlessSessionKey(null, null, false);
	}

	@Test
	public void testUnlessSessionKeyNoIfValueNullValue() throws Exception {
		testSkipOnUnlessSessionKey(null, "null", false);
	}

	@Test
	public void testUnlessSessionKeyNoIfValue2() throws Exception {
		testSkipOnUnlessSessionKey(null, "a", true);
	}

	public void testSkipOnIfParam(String ifValue, String paramValue, boolean expected) throws Exception {
		pipe.setIfParam("param");
		pipe.setIfValue(ifValue);
		pipe.addParameter(new Parameter("param", paramValue));
		configureAndStartPipe();
		assertEquals(expected, pipe.skipPipe(null, session));
	}

	@Test
	public void testIfParam() throws Exception {
		testSkipOnIfParam("a", "a", false);
	}

	@Test
	public void testIfParamNoMatch() throws Exception {
		testSkipOnIfParam("a", "b", true);
	}

	@Test
	public void testIfParamNullValue() throws Exception {
		testSkipOnIfParam("a", null, true);
	}

	@Test
	public void testIfParamNoIfValue1() throws Exception {
		testSkipOnIfParam(null, null, false);
	}

	@Test
	public void testIfParamNoIfValue2() throws Exception {
		testSkipOnIfParam(null, "a", true);
	}

}
