package org.frankframework.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.receivers.Receiver;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.SpringUtils;

class AdapterTest {
	private int pipeNr = 0;

	@Test
	void testComputeCombinedHideRegex() throws ConfigurationException {
		// Arrange
		try (Adapter adapter = new Adapter()) {
			adapter.refresh();
			PipeLine pipeLine = new PipeLine();

			IPipe p1 = buildTestPipe(pipeLine);
			IPipe p2 = buildTestPipe(pipeLine); // pipe will not have a hideRegex
			IPipe p3 = buildTestPipe(pipeLine);
			IPipe p4 = buildTestPipe(pipeLine);
			IPipe p5 = buildTestPipe(pipeLine);
			p1.setHideRegex("<pwd>.*?</pwd>");
			p4.setHideRegex("<pwd>.*?</pwd>"); // 2 pipes with same hideRegex
			p3.setHideRegex(".*?\\.pwd=.*");
			p5.setHideRegex(""); // Empty string should also be skipped

			adapter.setPipeLine(pipeLine);

			// Act
			String regex = adapter.computeCombinedHideRegex();

			// Assert
			assertEquals("(<pwd>.*?</pwd>)|(.*?\\.pwd=.*)", regex);

			// Act 2 -- test the regex
			String regexApplied = "<root><pwd>secret</pwd></root>";

			// Assert 2
			assertEquals("<root>hidden</root>", regexApplied.replaceFirst(regex, "hidden"));
		}
	}

	@Test
	void testDuplicateReceiverNames() {
		try (TestConfiguration config = new TestConfiguration(); Adapter adapter = config.createBean()) {

			Receiver<?> receiver1 = SpringUtils.createBean(adapter);
			receiver1.setName("testReceiver");

			Receiver<?> receiver2 = SpringUtils.createBean(adapter);
			receiver2.setName("testReceiver");

			adapter.addReceiver(receiver1);
			assertEquals(0, config.getConfigurationWarnings().size());

			adapter.addReceiver(receiver2);
			assertEquals(1, config.getConfigurationWarnings().size());
			assertEquals("Receiver [testReceiver] name must be unique!", config.getConfigWarning(0));
		}
	}

	private @NonNull EchoPipe buildTestPipe(@NonNull PipeLine pipeLine) throws ConfigurationException {
		EchoPipe pipe = new EchoPipe();
		pipe.setName("Pipe" + ++pipeNr);
		pipeLine.addPipe(pipe);
		return pipe;
	}
}
