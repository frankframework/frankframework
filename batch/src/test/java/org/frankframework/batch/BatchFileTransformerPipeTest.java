package org.frankframework.batch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.PipeTestBase;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestFileUtils;

/**
 *  Note: Not really testing the full features of the pipe, just testing some flows, to increase coverage.
 *  The pipe is deprecated and will be removed in the future.
 */
class BatchFileTransformerPipeTest extends PipeTestBase<BatchFileTransformerPipe> {

	@Override
	public BatchFileTransformerPipe createPipe() {
		return new BatchFileTransformerPipe();
	}

	@TempDir
	private Path tempDir;

	@Test
	void doPipeMovesFileAfterProcessing() throws Exception {
		URL inputUrl = TestFileUtils.getTestFileURL("/input.xml");
		File file = new File(inputUrl.toURI());
		Message input = new Message(file.getAbsolutePath());

		pipe.setDelete(false);
		pipe.configure();
		pipe.start();

		PipeRunResult pipeRunResult = pipe.doPipe(input, session);
		assertTrue(pipeRunResult.getResult().isEmpty());
		assertTrue(file.exists());
	}

	@Test
	void moveFileAfterProcessingDeletesFileWhenDeleteIsTrue() throws IOException, InterruptedException {
		File file = Files.createFile(tempDir.resolve("test.txt")).toFile();
		assertTrue(file.exists());

		BatchFileTransformerPipe.moveFileAfterProcessing(file, null, true, false, 0);
		assertFalse(file.exists());
	}

}
