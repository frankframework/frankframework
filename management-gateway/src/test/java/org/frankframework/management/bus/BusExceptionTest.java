package org.frankframework.management.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.core.IbisException;

public class BusExceptionTest {

	private static final String BUS_EXCEPTION_MESSAGE = "outer exception";

	public String expectedLogMessage;
	public Level expectedLogLevel;
	public Exception innerException;

	public static List<?> data() {
		return Arrays.asList(new Object[][] {
			{"", Level.WARN, null},
			{": cannot configure", Level.ERROR, new IbisException("cannot configure")},
			{": cannot configure: (IllegalStateException) something is wrong", Level.ERROR, new IbisException("cannot configure", new IllegalStateException("something is wrong"))},
		});
	}

	@ParameterizedTest
	@MethodSource("data")
	public void testExceptionMessage(String expectedLogMessage, Level expectedLogLevel, Exception innerException) {
		Exception ex = new BusException(BUS_EXCEPTION_MESSAGE, innerException);
		assertEquals(BUS_EXCEPTION_MESSAGE + expectedLogMessage, ex.getMessage());
	}
}
