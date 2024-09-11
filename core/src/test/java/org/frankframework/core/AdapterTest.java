package org.frankframework.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.annotation.Nonnull;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import org.jetbrains.annotations.NotNull;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.processors.CorePipeLineProcessor;
import org.frankframework.processors.CorePipeProcessor;
import org.frankframework.statistics.MetricsInitializer;
import org.frankframework.stream.Message;
import org.frankframework.util.RunState;

class AdapterTest {
	int pipeNr = 0;

	@Test
	void testComputeCombinedHideRegex() throws ConfigurationException {
		// Arrange
		Adapter adapter = new Adapter();
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

	private @Nonnull EchoPipe buildTestPipe(@Nonnull PipeLine pipeLine) throws ConfigurationException {
		EchoPipe pipe = new EchoPipe();
		pipe.setName("Pipe" + ++pipeNr);
		pipeLine.addPipe(pipe);
		return pipe;
	}

	@Test
	public void testAdapterExpectedSessionKeysAllPresent() throws ConfigurationException {
		// Arrange
		Adapter adapter = buildTestAdapter();

		PipeLineSession session = new PipeLineSession();
		session.put("k1", "v1");
		session.put("k2", "v2");
		session.put("k3", "v3");

		// Act // Assert
		assertDoesNotThrow(() -> adapter.processMessageWithExceptions("m1", Message.nullMessage(), session));
	}

	@Test
	public void testAdapterExpectedSessionKeysMissingKey() throws ConfigurationException {
		// Arrange
		Adapter adapter = buildTestAdapter();

		PipeLineSession session = new PipeLineSession();
		session.put("k1", "v1");

		// Act // Assert
		ListenerException e = assertThrows(ListenerException.class, () -> adapter.processMessageWithExceptions("m1", Message.nullMessage(), session));

		// Assert
		assertEquals("Adapter [Adapter] called without expected session keys [k2, k3]", e.getMessage());
	}

	private @NotNull Adapter buildTestAdapter() throws ConfigurationException {
		Adapter adapter = new Adapter() {
			@Override
			public RunState getRunState() {
				return RunState.STARTED;
			}
		};
		adapter.setName("Adapter");
		buildDummyPipeLine(adapter);
		MetricsInitializer configurationMetrics = mock();
		when(configurationMetrics.createCounter(any(), any())).then( ignored -> mock(Counter.class));
		when(configurationMetrics.createDistributionSummary(any(), any())).then( (ignored)-> mock(DistributionSummary.class));
		adapter.setConfigurationMetrics(configurationMetrics);
		adapter.setExpectsSessionKeys("k1, k2,k3");
		adapter.configure();
		return adapter;
	}

	private void buildDummyPipeLine(Adapter adapter) throws ConfigurationException {
		PipeLine pipeLine = new PipeLine();
		pipeLine.setConfigurationMetrics(mock());
		CorePipeLineProcessor pipeLineProcessor = new CorePipeLineProcessor();
		pipeLineProcessor.setPipeProcessor(new CorePipeProcessor());
		pipeLine.setPipeLineProcessor(pipeLineProcessor);
		EchoPipe pipe = buildTestPipe(pipeLine);
		pipeLine.setFirstPipe(pipe.getName());
		adapter.setPipeLine(pipeLine);
	}
}
