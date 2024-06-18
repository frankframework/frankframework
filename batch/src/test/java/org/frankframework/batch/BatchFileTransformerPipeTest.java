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

class BatchFileTransformerPipeTest extends PipeTestBase<BatchFileTransformerPipe> {

	@Override
	public BatchFileTransformerPipe createPipe() {
		return new BatchFileTransformerPipe();
	}

	@TempDir
	private Path tempDir;

	@Test // Not really using the full features of the pipe, just testing some flows
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
