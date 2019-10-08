/*
   Copyright 2019 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

public class ConfigurationUtilsTest {

	@Test
	public void retrieveBuildInfo() throws IOException {
		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull("BuildInfoZip not found", zip);

		ConfigurationUtils.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "";
		String[] buildInfo = ConfigurationUtils.retrieveBuildInfo(zip.openStream());

		String buildInfoName = buildInfo[0];
		assertEquals("buildInfo name does not match", "ConfigurationName", buildInfoName);

		String buildInfoVersion = buildInfo[1];
		assertEquals("buildInfo version does not match", "001_20191002-1300", buildInfoVersion);
	}

	@Test
	public void retrieveBuildInfoSC() throws IOException {
		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull("BuildInfoZip not found", zip);

		ConfigurationUtils.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "_SC";
		String[] buildInfo = ConfigurationUtils.retrieveBuildInfo(zip.openStream());

		String buildInfoName = buildInfo[0];
		assertEquals("buildInfo name does not match", "ConfigurationName", buildInfoName);

		String buildInfoVersion = buildInfo[1];
		assertEquals("buildInfo version does not match", "123_20181002-1300", buildInfoVersion);
	}

	@Test
	public void retrieveBuildInfoCUSTOM() throws IOException {
		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull("BuildInfoZip not found", zip);

		ConfigurationUtils.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "_SPECIAL";
		String[] buildInfo = ConfigurationUtils.retrieveBuildInfo(zip.openStream());

		String buildInfoName = buildInfo[0];
		assertEquals("buildInfo name does not match", "ConfigurationName", buildInfoName);

		String buildInfoVersion = buildInfo[1];
		assertEquals("buildInfo version does not match", "789_20171002-1300", buildInfoVersion);
	}
}
