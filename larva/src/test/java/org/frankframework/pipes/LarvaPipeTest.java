package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.AppConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LarvaPipeTest extends PipeTestBase<LarvaPipe> {

	private final TestConfiguration configuration = new TestConfiguration();
	@TempDir
	private Path tempDir;

	@Override
	public LarvaPipe createPipe() {
		return new LarvaPipe();
	}

	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		AppConstants.getInstance().setProperty("webapp.realpath", tempDir.toString() + File.separatorChar);
		adapter.setConfiguration(configuration);
		pipeline.setAdapter(adapter);
		pipeline.configure();
		pipeline.start();
	}

	@Test
	public void testPlain() throws Exception {
		configureAndStartPipe();
		assertFalse(pipe.skipPipe(null, session));
	}

	@Test
	public void testDoPipeWithoutScenarios() throws PipeRunException, IOException {
		// Act
		PipeRunResult prr = doPipe(pipe, null, session);
		String result = Message.asString(prr.getResult());

		// Assert
		assertEquals("No scenarios root directories found\n", result);
	}
}
