package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.functional.ThrowingSupplier;
import org.frankframework.stream.Message;

class MessageToStringResolverTest {

	private Map<String, Object> propsA;
	private Map<String, Object> propsB;
	private Message message;

	@BeforeEach
	void setUp() {
		propsA = new HashMap<>();
		propsB = new HashMap<>();
		message = new Message("My Message");
		propsA.put("msg", message);
		propsB.put("msg", "Not My Message");
	}

	@Test
	void testStringSubstitutionWithMessageInSingleProps() {
		// Act
		String result = StringResolver.substVars("lalala ${msg} tralala", propsA);

		// Assert
		assertEquals("lalala My Message tralala", result);
	}

	@Test
	void testStringSubstitutionWithMessageInProps1() {
		// Act
		String result = StringResolver.substVars("lalala ${msg} tralala", propsA, propsB);

		// Assert
		assertEquals("lalala My Message tralala", result);
	}

	@Test
	void testStringSubstitutionWithMessageInProps2() {
		// Act
		String result = StringResolver.substVars("lalala ${msg} tralala", propsB, propsA);

		// Assert
		assertEquals("lalala My Message tralala", result);
	}

	@Test
	void testStringSubstitutionWithMessageNotInProps() {
		// Act
		String result = StringResolver.substVars("lalala ${no-such-msg} tralala", propsA);

		// Assert
		assertEquals("lalala  tralala", result);
	}

	@Test
	void testWithExceptionInMessage() {
		// Arrange
		Message badMessage = Message.asMessage((ThrowingSupplier)()-> Files.newInputStream(Paths.get("this file does not exist")));
		propsA.put("bad-message", badMessage);

		// Act
		String result = StringResolver.substVars("lalala ${bad-message} tralala", propsA);

		// Assert
		assertEquals("lalala  tralala", result);
	}
}
