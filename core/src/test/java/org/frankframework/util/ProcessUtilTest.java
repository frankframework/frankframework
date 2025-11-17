package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.frankframework.core.TimeoutException;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestAssertions;

public class ProcessUtilTest {

	@Test
	public void happyFlow() throws TimeoutException, IOException {
		final List<String> command;
		if (TestAssertions.isTestRunningOnWindows()) {
			command = List.of("cmd", "/C", "echo", "test");
		} else {
			command = List.of("echo", "test");
		}

		Message result = ProcessUtil.executeCommand(command, 2);
		try (result) {
			assertEquals("test", result.asString().trim());
		}
	}

	@Test
	public void timeout() {
		final List<String> command;
		if (TestAssertions.isTestRunningOnWindows()) {
			command = List.of("cmd", "echo", "test");
		} else {
			command = List.of("sleep", "5");
		}

		TimeoutException ex = assertThrows(TimeoutException.class, () -> ProcessUtil.executeCommand(command, 2));
		assertEquals("command ["+String.join(" ", command)+"] timed out: (InterruptedException)", ex.getMessage());
	}

	@Test
	public void invalidCommand() {
		List<String> invalidCommand = List.of("!@$#%^&");
		IOException ex = assertThrows(IOException.class, () -> ProcessUtil.executeCommand(invalidCommand, 2));
		assertEquals("unable to execute command [!@$#%^&]", ex.getMessage());
	}
}
