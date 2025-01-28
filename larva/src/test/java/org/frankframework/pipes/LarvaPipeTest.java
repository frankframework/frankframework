package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.frankframework.core.PipeRunResult;
import org.frankframework.larva.LarvaLogLevel;
import org.frankframework.util.AppConstants;

class LarvaPipeTest extends PipeTestBase<LarvaPipe> {

	@TempDir
	private Path tempDir;

	@Override
	public LarvaPipe createPipe() {
		return new LarvaPipe();
	}

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		AppConstants.getInstance().setProperty("webapp.realpath", tempDir.toString() + File.separatorChar);
	}

	@Test
	public void testDoPipeWithoutScenarios() throws Exception {
		// Arrange
		pipe.setLogLevel(LarvaLogLevel.DEBUG);
		configureAndStartPipe();

		// Act
		PipeRunResult prr = doPipe(pipe, null, session);
		String result = prr.getResult().asString();

		// Assert
		assertEquals("No scenarios root directories found\n", result);
	}
}
