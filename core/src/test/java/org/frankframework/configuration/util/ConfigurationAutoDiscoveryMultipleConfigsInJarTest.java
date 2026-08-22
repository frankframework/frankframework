/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.configuration.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.classloaders.IConfigurationClassLoader;
import org.frankframework.configuration.classloaders.JarFileClassLoader;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.AppConstants;

class ConfigurationAutoDiscoveryMultipleConfigsInJarTest {

	@BeforeAll
	static void setUp() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/ClassLoader/MultipleConfigsInJar");
		assertNotNull(url);
		File directory = new File(url.toURI());
		AppConstants.getInstance().setProperty("configurations.directory", directory.getCanonicalPath());
	}

	@AfterAll
	static void removeAppConstantsInstance() {
		AppConstants.removeInstance();
	}

	@Test
	void retrieveAllConfigNamesTestWithFS() throws IOException {
		try (TestConfiguration applicationContext = new TestConfiguration()) {
			ConfigurationAutoDiscovery autoDiscovery = applicationContext.createBean();
			autoDiscovery.withDirectoryScanner();

			Map<String, Class<? extends IConfigurationClassLoader>> configs = autoDiscovery.scan(true);

			assertThat("keyset was: " + configs.keySet(), configs.keySet(), IsIterableContainingInOrder.contains("IAF_Util", "TestConfiguration", "Weer", "Nieuws", "Trein"));

			assertNull(configs.get("IAF_Util"));
			assertNull(configs.get("TestConfiguration"));

			assertEquals(JarFileClassLoader.class, configs.get("Weer"));
		}
	}
}
