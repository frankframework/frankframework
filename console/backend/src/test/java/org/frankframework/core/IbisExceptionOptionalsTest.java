package org.frankframework.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Test whether the optional dependencies in iaf-common are truly optional.
 */
public class IbisExceptionOptionalsTest {

	private static class IbisExceptionSubClass extends IbisException {
		public IbisExceptionSubClass(Throwable t) {
			this("", t);
		}
		public IbisExceptionSubClass(String errMsg, Throwable t) {
			super("prefix"+errMsg, t);
		}
	}

	@BeforeAll
	public static void ensureOptionalDependenciesAreNotOnTheClasspath() {
		assertAll(
			() -> assertFalse(isPresent("jakarta.mail.internet.AddressException"), "found AddressException on the classpath, unable to test optional dependency"),
			() -> assertFalse(isPresent("oracle.jdbc.xa.OracleXAException"), "found OracleXAException on the classpath, unable to test optional dependency")
		);
	}

	@Test
	void twoNestedExceptionsWithDifferentMessages() {
		IbisExceptionSubClass exception = new IbisExceptionSubClass("Some text here", new NullPointerException("some other text here"));
		String result = exception.getMessage();

		assertEquals("prefixSome text here: (NullPointerException) some other text here", result);
	}

	@Test
	void twoNestedExceptionsWithTheSameMessage() {
		IbisExceptionSubClass exception = new IbisExceptionSubClass(new NullPointerException("Some other text here"));
		String result = exception.getMessage();

		assertEquals("prefix: (NullPointerException) Some other text here", result);
	}

	public static boolean isPresent(String className) {
		try {
			Class.forName(className);
			return true;
		} catch (Throwable ignored) {
			// Class or one of its dependencies is not present...
			return false;
		}
	}
}
