package org.frankframework.configuration.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FilterInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;

public class BuildInfoValidatorTest {

	@Test
	public void retrieveBuildInfo() throws Exception {
		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull(zip, "BuildInfoZip not found");

		BuildInfoValidator.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "";
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

		assertTrue(wasClosed.get());  //Ensure the original stream is closed properly
		assertTrue(details.getJar().read() > -1);
		assertTrue(details.getJar().read() > -1); //Allow multiple reads
	}

	@Test
	public void retrieveBuildInfoSC() throws Exception {
		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull(zip, "BuildInfoZip not found");

		BuildInfoValidator.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "_SC";
		BuildInfoValidator details = new BuildInfoValidator(zip.openStream());

		assertEquals("ConfigurationName", details.getName(), "buildInfo name does not match");
		assertEquals("123_20181002-1300", details.getVersion(), "buildInfo version does not match");
	}

	@Test
	public void retrieveBuildInfoCUSTOM() throws Exception {
		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull(zip, "BuildInfoZip not found");

		BuildInfoValidator.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "_SPECIAL";
		BuildInfoValidator details = new BuildInfoValidator(zip.openStream());

		assertEquals("ConfigurationName", details.getName(), "buildInfo name does not match");
		assertEquals("789_20171002-1300", details.getVersion(), "buildInfo version does not match");
	}

	@Test
	public void configurationValidator() throws Exception {
		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull(zip, "BuildInfoZip not found");

		BuildInfoValidator.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "";
		BuildInfoValidator details = new BuildInfoValidator(zip.openStream());

		assertEquals("ConfigurationName", details.getName(), "buildInfo name does not match");
		assertEquals("001_20191002-1300", details.getVersion(), "buildInfo version does not match");
	}

	@Test
	public void configurationValidatorNoBuildInfoZip() throws Exception {
		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/noBuildInfoZip.jar");
		assertNotNull(zip, "BuildInfoZip not found");

		BuildInfoValidator.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "";

		assertThrows(ConfigurationException.class, () -> new BuildInfoValidator(zip.openStream()));
	}
}
