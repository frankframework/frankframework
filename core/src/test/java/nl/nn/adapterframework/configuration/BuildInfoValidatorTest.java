package nl.nn.adapterframework.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;

import org.junit.Test;

public class BuildInfoValidatorTest {

	@Test
	public void retrieveBuildInfo() throws Exception {
		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull("BuildInfoZip not found", zip);

		BuildInfoValidator.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "";
		BuildInfoValidator details = new BuildInfoValidator(zip.openStream());

		assertEquals("buildInfo name does not match", "ConfigurationName", details.getName());
		assertEquals("buildInfo version does not match", "001_20191002-1300", details.getVersion());
	}

	@Test
	public void retrieveBuildInfoSC() throws Exception {
		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull("BuildInfoZip not found", zip);

		BuildInfoValidator.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "_SC";
		BuildInfoValidator details = new BuildInfoValidator(zip.openStream());

		assertEquals("buildInfo name does not match", "ConfigurationName", details.getName());
		assertEquals("buildInfo version does not match", "123_20181002-1300", details.getVersion());
	}

	@Test
	public void retrieveBuildInfoCUSTOM() throws Exception {
		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull("BuildInfoZip not found", zip);

		BuildInfoValidator.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "_SPECIAL";
		BuildInfoValidator details = new BuildInfoValidator(zip.openStream());

		assertEquals("buildInfo name does not match", "ConfigurationName", details.getName());
		assertEquals("buildInfo version does not match", "789_20171002-1300", details.getVersion());
	}

	@Test
	public void configurationValidator() throws Exception {
		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull("BuildInfoZip not found", zip);

		BuildInfoValidator.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "";
		BuildInfoValidator details = new BuildInfoValidator(zip.openStream());

		assertEquals("buildInfo name does not match", "ConfigurationName", details.getName());
		assertEquals("buildInfo version does not match", "001_20191002-1300", details.getVersion());
	}

	@Test(expected=ConfigurationException.class)
	public void configurationValidatorNoBuildInfoZip() throws Exception {
		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/noBuildInfoZip.jar");
		assertNotNull("BuildInfoZip not found", zip);

		BuildInfoValidator.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "";
		new BuildInfoValidator(zip.openStream());
	}
}
