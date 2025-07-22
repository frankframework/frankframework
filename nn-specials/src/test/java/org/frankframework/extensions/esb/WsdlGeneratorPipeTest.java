package org.frankframework.extensions.esb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.frankframework.testutil.TestAssertions;
import org.frankframework.util.TimeProvider;

class WsdlGeneratorPipeTest {

	@TempDir
	public static Path testFolder;

	@Test
	void createTempDirectoryTest() throws Exception {
		File f = new File(testFolder.toString());
		File file = WsdlGeneratorPipe.createTempDirectory(f);
		boolean b = file.exists();
		Files.deleteIfExists(file.toPath());
		assertTrue(b);
	}

	@Test //retrieve the first file from a directory. Alphabetically it should first return 'copyFile'. Add a stability period, and check if it skips the first file
	void testGetFirstFile() {
		assumeTrue(TestAssertions.isTestRunningOnWindows());

		long stabilityPeriod = 5000;
		String base = "/GetFirstFile/";
		File copyFromFile = new File(this.getClass().getResource(base + "copyFrom.txt").getPath());
		File copyFile = new File(this.getClass().getResource(base + "copyFile.txt").getPath());
		copyFromFile.setLastModified(TimeProvider.nowAsMillis()-(stabilityPeriod + 500)); //mark file as stable (add 500ms to the stability period because of HDD latency)
		copyFile.setLastModified(TimeProvider.nowAsMillis()); //update last modified to now, so it fails the stability period

		File directory = new File(this.getClass().getResource(base).getPath());
		File file = WsdlGeneratorPipe.getFirstFile(directory);
		assertEquals("copyFile.txt", file.getName());
	}

}
