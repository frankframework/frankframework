package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import jakarta.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.Parameter;
import org.frankframework.stream.Message;

public class FixedForwardPipeTest extends PipeTestBase<FixedForwardPipe> {

	@Override
	public FixedForwardPipe createPipe() throws ConfigurationException {
		return new FixedForwardPipe() {
			@Nonnull
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

	public static Stream<Arguments> testSkipOnOnlyIfSessionKey() {
		return Stream.of(
				arguments("a", "a", false),
				arguments("a", Message.asMessage("a"), false),
				arguments("a", "b", true),
				arguments("a", null, true),
				arguments("a", Message.nullMessage(), true),
				arguments("a", "null", true),
				arguments("a", Message.asMessage("null"), true),
				arguments(null, null, true),
				arguments(null, "null", true),
				arguments(null, "a", false),
				arguments("true", "true", false),
				arguments("true", Message.asMessage("true"), false),
				arguments("true", "false", true),
				arguments("true", Message.asMessage("false"), true),
				arguments("true", true, false),
				arguments("true", Message.asMessage(true), false),
				arguments("true", false, true),
				arguments("true", Message.asMessage(false), true),
				arguments("1", 1, false),
				arguments("1", Message.asMessage(1), false),
				arguments("1", 2, true),
				arguments("1", Message.asMessage(2), true)
		);
	}

	@ParameterizedTest
	@MethodSource
	public void testSkipOnOnlyIfSessionKey(String onlyIfValue, Object sessionKeyValue, boolean expected) throws Exception {
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


	public static Stream<Arguments> testSkipOnUnlessSessionKey() {
		return Stream.of(
				arguments("a", "a", true),
				arguments("a", Message.asMessage("a"), true),
				arguments("a", "b", false),
				arguments("a", null, false),
				arguments("a", Message.nullMessage(), false),
				arguments("a", "null", false),
				arguments("a", Message.asMessage("null"), false),
				arguments(null, null, false),
				arguments(null, "null", false),
				arguments(null, "a", true),
				arguments("true", true, true),
				arguments("true", Message.asMessage(true), true),
				arguments("true", false, false),
				arguments("true", Message.asMessage(false), false),
				arguments("1", 1, true),
				arguments("1", Message.asMessage(1), true),
				arguments("1", 2, false),
				arguments("1", Message.asMessage(2), false)
		);
	}

	@ParameterizedTest
	@MethodSource
	public void testSkipOnUnlessSessionKey(String unlessValue, Object sessionKeyValue, boolean expected) throws Exception {
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

	public static Stream<Arguments> testSkipOnIfParam() {
		return Stream.of(
				arguments("a", "a", false),
				arguments("a", Message.asMessage("a"), false),
				arguments("a", "b", true),
				arguments("a", null, true),
				arguments("a", Message.nullMessage(), true),
				arguments("a", "null", true),
				arguments("a", Message.asMessage("null"), true),
				arguments(null, null, false),
				arguments(null, "null", true),
				arguments(null, "a", true),
				arguments("true", "true", false),
				arguments("true", Message.asMessage("true"), false),
				arguments("true", "false", true),
				arguments("true", Message.asMessage("false"), true),
				arguments("true", true, false),
				arguments("true", Message.asMessage(true), false),
				arguments("true", false, true),
				arguments("true", Message.asMessage(false), true),
				arguments("1", 1, false),
				arguments("1", Message.asMessage(1), false),
				arguments("1", 2, true),
				arguments("1", Message.asMessage(2), true)
		);
	}

	@ParameterizedTest
	@MethodSource
	public void testSkipOnIfParam(String ifValue, Object paramValue, boolean expected) throws Exception {
		// We get the param-value via the session key so that we test with non-string values in the parameter
		pipe.setIfParam("param");
		pipe.setIfValue(ifValue);
		Parameter param = new Parameter();
		param.setName("param");
		param.setSessionKey("paramSessionKey");
		session.put("paramSessionKey", paramValue);
		pipe.addParameter(param);
		configureAndStartPipe();
		assertEquals(expected, pipe.skipPipe(null, session));
	}
}
