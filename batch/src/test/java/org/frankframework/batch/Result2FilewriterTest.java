package org.frankframework.batch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.Writer;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.frankframework.core.PipeLineSession;

/**
 *  Note: Not really testing the full features of the pipe, just testing some flows, to increase coverage.
 *  The writer is deprecated and will be removed in the future.
 */
class Result2FilewriterTest {

	private final Result2Filewriter writer = new Result2Filewriter();

	@TempDir
	private Path tempDir;

	@Test // Just some basic tests. Not a complete test of the class.
	void createWriterCreatesNewFile() throws Exception {
		writer.setOutputDirectory(tempDir.toString());
		writer.setFilenamePattern("test.txt");
		PipeLineSession session = new PipeLineSession();

		try(Writer w = writer.createWriter(session, "writer1")) {
			w.write("test");
		}
		String filePath = writer.finalizeResult(session, "writer1", false);

		File file = new File(filePath);
		assertTrue(file.exists());
	}

	@Test
	void finalizeResultDeletesFile() throws Exception {
		writer.setOutputDirectory(tempDir.toString());
		writer.setFilenamePattern("test.txt");

		PipeLineSession session = new PipeLineSession();

		try(Writer w = writer.createWriter(session, "streamId")) {
			w.write("test");
		}
		writer.finalizeResult(session, "streamId", true);

		assertFalse(new File(tempDir.toFile(), "test.txt").exists());
	}

}
