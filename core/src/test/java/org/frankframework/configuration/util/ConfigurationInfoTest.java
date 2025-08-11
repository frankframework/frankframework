package org.frankframework.configuration.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.junit.jupiter.api.Test;

import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.DateFormatUtils;

public class ConfigurationInfoTest {

	@Test
	public void testDate() {
		ConfigurationInfo info = new ConfigurationInfo("name", "version", "20100526-1145");
		assertEquals(Instant.parse("2010-05-26T11:45:00.000Z"), info.getTimestamp());
	}

	@Test
	public void parseManifestFile() throws IOException {
		Instant expectedDate = DateFormatUtils.parseGenericDate("2025-08-09 12:03");
		URL manifestFile = TestFileUtils.getTestFileURL("/ConfigurationUtils/MANIFEST.MF");
		assertNotNull(manifestFile);
		Manifest manifest = new Manifest(manifestFile.openStream());
		ConfigurationInfo info = new ConfigurationInfo(manifest);

		assertAll(
				() -> assertEquals("Configuration_Template", ConfigurationInfo.fromManifest(manifest).getName()),
				() -> assertEquals("Configuration_Template", info.getName()),
				() -> assertEquals(new DefaultArtifactVersion("0.0.1-SNAPSHOT"), info.getVersion()),
				() -> assertEquals(expectedDate, info.getTimestamp()), // 2025-08-09 12:03
				() -> assertEquals("jar-configuration", info.getArtifactId()),
				() -> assertEquals("org.frankframework", info.getGroupId()),
				() -> assertEquals("FrankFramework! ConfigurationJar", info.getDescription()),
				() -> assertEquals("FrankFramework!", info.getOrganisation()),
				() -> assertEquals(new DefaultArtifactVersion("9.2.0"), info.getFrameworkVersion())
		);
	}

	@Test
	public void testEmptyManifestFile() throws IOException {
		URL zip = BuildInfoValidatorTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull(zip, "Config JAR not found");

		try (JarInputStream jarInputStream = new JarInputStream(zip.openStream())) {
			assertNotNull(jarInputStream.getManifest(), "config has no valid manifest file"); // Ensure the jar has a metainf.md file
			ConfigurationInfo info = new ConfigurationInfo(jarInputStream.getManifest());
			assertNull(info.getName());
		}
	}
}
