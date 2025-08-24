package org.frankframework.configuration.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FilterInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarInputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.testutil.TestAppender;

public class BuildInfoValidatorTest {

	@BeforeEach
	@AfterEach
	public void restFileSuffix() {
		BuildInfoValidator.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "";
	}

	@Test
	public void retrieveBuildInfo() throws Exception {
		URL zip = BuildInfoValidatorTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull(zip, "BuildInfoZip not found");

		AtomicBoolean wasClosed = new AtomicBoolean(false);
		FilterInputStream fis = new FilterInputStream(zip.openStream()) {
			@Override
			public void close() throws IOException {
				wasClosed.set(true);
				super.close();
			}
		};

		BuildInfoValidator details = new BuildInfoValidator(fis);

		assertEquals("ConfigurationName", details.getName(), "buildInfo name does not match");
		assertEquals("001_20191002-1300", details.getVersion(), "buildInfo version does not match");

		assertTrue(wasClosed.get());  // Ensure the original stream is closed properly
		assertTrue(details.getJar().read() > -1);
		assertTrue(details.getJar().read() > -1); // Allow multiple reads
	}

	@Test
	public void retrieveBuildInfoSC() throws Exception {
		URL zip = BuildInfoValidatorTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull(zip, "BuildInfoZip not found");

		BuildInfoValidator.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "_SC";
		BuildInfoValidator details = new BuildInfoValidator(zip.openStream());

		assertEquals("ConfigurationName", details.getName(), "buildInfo name does not match");
		assertEquals("123_20181002-1300", details.getVersion(), "buildInfo version does not match");
	}

	@Test
	public void retrieveBuildInfoCUSTOM() throws Exception {
		URL zip = BuildInfoValidatorTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull(zip, "BuildInfoZip not found");

		BuildInfoValidator.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "_SPECIAL";
		BuildInfoValidator details = new BuildInfoValidator(zip.openStream());

		assertEquals("ConfigurationName", details.getName(), "buildInfo name does not match");
		assertEquals("789_20171002-1300", details.getVersion(), "buildInfo version does not match");
	}

	@Test
	public void configurationValidator() throws Exception {
		// This JAR has a METAINF file that's practically empty. It should be skipped.
		URL zip = BuildInfoValidatorTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull(zip, "BuildInfoZip not found");

		BuildInfoValidator details = new BuildInfoValidator(zip.openStream());

		assertEquals("ConfigurationName", details.getName(), "buildInfo name does not match");
		assertEquals("001_20191002-1300", details.getVersion(), "buildInfo version does not match");
	}

	@Test
	public void configurationValidatorNoBuildInfoZip() throws Exception {
		URL zip = BuildInfoValidatorTest.class.getResource("/ConfigurationUtils/noBuildInfoZip.jar");
		assertNotNull(zip, "BuildInfoZip not found");

		assertThrows(ConfigurationException.class, () -> new BuildInfoValidator(zip.openStream()));
	}

	@Test
	public void configurationWithEmptyMetaInf() throws Exception {
		URL zip = BuildInfoValidatorTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull(zip, "Config JAR not found");

		try (JarInputStream jarInputStream = new JarInputStream(zip.openStream())) {
			assertNotNull(jarInputStream.getManifest(), "config has no valid manifest file"); // Ensure the jar has a manifest.md file
		}

		BuildInfoValidator details = new BuildInfoValidator(zip.openStream());

		assertEquals("ConfigurationName", details.getName(), "buildInfo name does not match");
		assertEquals("001_20191002-1300", details.getVersion(), "buildInfo version does not match");
	}

	@Test
	public void configurationWithMetaInf() throws Exception {
		// This jar also has a buildinfo.properties file, but it should be ignored.
		URL jar = BuildInfoValidatorTest.class.getResource("/ConfigurationUtils/configjar-template-0.0.1.jar");
		assertNotNull(jar, "Config JAR not found");

		try (JarInputStream jarInputStream = new JarInputStream(jar.openStream())) {
			assertNotNull(jarInputStream.getManifest(), "config has no valid manifest file"); // Ensure the jar has a manifest.md file
		}

		BuildInfoValidator details = new BuildInfoValidator(jar.openStream());

		assertEquals("Configuration_Template", details.getName(), "buildInfo name does not match");
		assertEquals("0.0.1-SNAPSHOT_20250811-0709", details.getVersion(), "buildInfo version does not match");

		// No version boundary, allow all.
		assertDoesNotThrow(() -> details.validate("9.2.0"));
		assertDoesNotThrow(() -> details.validate("9.1.0"));
		assertDoesNotThrow(() -> details.validate("9.2.0-SNAPSHOT"));
		assertDoesNotThrow(() -> details.validate("9.2.0"));
		assertDoesNotThrow(() -> details.validate("9.2.5"));
		assertDoesNotThrow(() -> details.validate("9.3.0-SNAPSHOT"));
		assertDoesNotThrow(() -> details.validate("9.3.0"));
		assertDoesNotThrow(() -> details.validate("10.0.0"));
	}

	@Test
	public void configurationWithMetaInfAndVersionLimit() throws Exception {
		// This jar also has a buildinfo.properties file, but it should be ignored.
		URL jar = BuildInfoValidatorTest.class.getResource("/ConfigurationUtils/configjar-restricted-ffversion-0.0.1.jar");
		assertNotNull(jar, "Config JAR not found");

		try (JarInputStream jarInputStream = new JarInputStream(jar.openStream())) {
			assertNotNull(jarInputStream.getManifest(), "config has no valid manifest file"); // Ensure the jar has a manifest.md file
		}

		BuildInfoValidator details = new BuildInfoValidator(jar.openStream());

		assertEquals("Configuration_Template", details.getName(), "buildInfo name does not match");
		assertEquals("0.0.1-SNAPSHOT_20250823-1703", details.getVersion(), "buildInfo version does not match");

		// Since the lower limit is 9.2 these should throw
		assertThrows(ConfigurationException.class, () -> details.validate("9.1.0"));
		assertThrows(ConfigurationException.class, () -> details.validate("9.2.0-SNAPSHOT"));

		// These versions are 'newer' and do not throw
		assertDoesNotThrow(() -> details.validate("9.2.0"));
		assertDoesNotThrow(() -> details.validate("9.2.5"));
		assertDoesNotThrow(() -> details.validate("9.3.0-SNAPSHOT"));
		assertDoesNotThrow(() -> details.validate("9.3.0"));
		assertDoesNotThrow(() -> details.validate("10.0.0"));
	}

	@Test
	public void configurationWithMetaInfButNoConfigFolder() throws Exception {
		// This jar also has a buildinfo.properties file, but it should be ignored.
		URL jar = BuildInfoValidatorTest.class.getResource("/ConfigurationUtils/faulty-configjar-0.0.1.jar");
		assertNotNull(jar, "Config JAR not found");

		try (JarInputStream jarInputStream = new JarInputStream(jar.openStream())) {
			assertNotNull(jarInputStream.getManifest(), "config has no valid manifest file"); // Ensure the jar has a manifest.md file
		}

		try (TestAppender appender = TestAppender.newBuilder().build()) {
			ConfigurationException ex = assertThrows(ConfigurationException.class, () -> new BuildInfoValidator(jar.openStream()));
			assertEquals("unable to read jarfile: (IOException) no (valid) [META-INF/MANIFEST.MF] or [BuildInfo.properties] present in configuration", ex.getMessage());
			assertTrue(appender.contains("did find a MANIFEST file but not a valid configuration folder in [Configuration_Template]"));
		}
	}
}
