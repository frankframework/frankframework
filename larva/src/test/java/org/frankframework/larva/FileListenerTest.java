package org.frankframework.larva;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;

import org.junit.jupiter.api.Test;

class FileListenerTest {

	@Test
	void testIsFileBinaryEqual() throws Exception {
		URL testFileURL = getClass().getResource("/file.txt");
		URL testFileURL2 = getClass().getResource("/copyFile.txt");
		File testFile = new File(testFileURL.getPath());
		File testFile2 = new File(testFileURL2.getPath());

		assertTrue(FileListener.isFileBinaryEqual(testFile, testFile));
		assertFalse(FileListener.isFileBinaryEqual(testFile, testFile2));
	}

}
