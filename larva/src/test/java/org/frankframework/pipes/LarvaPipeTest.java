package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeRunResult;
import org.frankframework.larva.LarvaLogLevel;

class LarvaPipeTest extends PipeTestBase<LarvaPipe> {

	@Override
	public LarvaPipe createPipe() {
		return new LarvaPipe();
	}

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
	}

	@Test
	public void testDoPipeWithoutScenarios() throws Exception {
		// Arrange
		pipe.setLogLevel(LarvaLogLevel.ERROR);
		configureAndStartPipe();

		// Act
		PipeRunResult prr = doPipe(pipe, null, session);
		String result = prr.getResult().asString().trim();

		// Assert
		assertEquals("ERROR: No scenarios root directories found", result);
	}
}
