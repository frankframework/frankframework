/*
   Copyright 2018 Nationale-Nederlanden

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
package nl.nn.adapterframework.configuration.classloaders;

import java.net.URL;
import java.util.jar.JarFile;

import org.junit.Test;

import nl.nn.adapterframework.configuration.classloaders.JarFileClassLoader;
import static org.junit.Assert.*;

public class JarFileClassLoaderTest extends ClassLoaderTestBase<JarFileClassLoader> {

	private final String JAR_FILE = "/classLoader-test.zip";

	@Override
	protected String getScheme() {
		return "bytesclassloader";
	}

	@Override
	JarFileClassLoader createClassLoader(ClassLoader parent) throws Exception {
		URL file = this.getClass().getResource(JAR_FILE);
		assertNotNull("jar url not found", file);
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull("jar file not found",jarFile);

		JarFileClassLoader cl = new JarFileClassLoader(parent);
		cl.setJar(file.getFile());
		appConstants.put("configurations."+getConfigurationName()+".jar", file.getFile());
		return cl;
	}

	/* test files that are only present in the JAR_FILE zip */
	@Test
	public void classloaderOnlyFile() {
		resourceExists("dummy.xml");
	}

	@Test
	public void classloaderOnlyFolder() {
		resourceExists("folder/dummy.xml");
	}
}
