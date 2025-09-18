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
package org.frankframework.configuration.classloaders;

import java.net.URL;

import org.junit.jupiter.api.Test;

public class DirectoryClassLoaderTest extends ConfigurationClassLoaderTestBase<DirectoryClassLoader> {

	@Override
	public DirectoryClassLoader createClassLoader(ClassLoader parent) throws Exception {
		URL file = this.getClass().getResource("/ClassLoader/DirectoryClassLoaderRoot");

		DirectoryClassLoader cl = new DirectoryClassLoader(parent);
		cl.setDirectory(file.getFile());
		String key = "configurations."+getConfigurationName()+".directory";
		appConstants.setProperty(key, file.getFile());
		return cl;
	}

	/* test files that are only present in the JAR_FILE zip */
	@Test
	@SuppressWarnings("java:S2699") // Method contains indirect assertion
	public void classloaderOnlyFile() {
		resourceExists("fileOnlyOnDirectoryClassPath.xml");
	}

	@Test
	@SuppressWarnings("java:S2699") // Method contains indirect assertion
	public void classloaderOnlyFolder() {
		resourceExists("ClassLoader/fileOnlyOnDirectoryClassPath.xml");
	}
}
