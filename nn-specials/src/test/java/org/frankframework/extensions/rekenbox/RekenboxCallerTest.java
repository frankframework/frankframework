package org.frankframework.extensions.rekenbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class RekenboxCallerTest {

	@TempDir
	private static Path tempDir;

	private static Path file;

	@BeforeAll
	public static void setUp() throws IOException {
		file = Files.createFile(tempDir.resolve("nnspecials.txt"));
	}

	@Test
	public void testFileToStringFileNameEndLine() throws Exception {
		// Misc.resourceToString()
		writeToTestFile();
		assertEquals("inside the lebron file", RekenBoxCallerPipe.fileToString(file.toString(), " the end", false));
	}

	private void writeToTestFile() throws IOException {
		Writer w = new FileWriter(file.toString());
		w.write("inside the lebron file");
		w.close();
	}
}
