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

import static org.junit.Assert.*;

public class JarFileClassLoaderTest extends ConfigurationClassLoaderTestBase<JarFileClassLoader> {

	@Override
	protected String getScheme() {
		return "classpath";
	}

	@Override
	public JarFileClassLoader createClassLoader(ClassLoader parent) throws Exception {
		URL file = this.getClass().getResource(JAR_FILE);
		assertNotNull("jar url ["+JAR_FILE+"] not found", file);
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
		resourceExists("fileOnlyOnZipClassPath.xml");
	}

	@Test
	public void classloaderOnlyFolder() {
		resourceExists("ClassLoader/fileOnlyOnZipClassPath.xml");
	}

	@Test
	public void checkFilePathWithoutBasePath() throws Exception {
		String filename = "fileOnlyOnZipClassPath.xml";

		createAndConfigure("ClassLoader"); //Create a ClassLoader with BasePath

		URL url = getResource(filename);
		assertEquals("Path of resource invalid", filename, url.getPath());
	}

	@Test
	public void checkFilePathWithBasePath() throws Exception {
		String fileNameWithBasePath = "ClassLoader/fileOnlyOnZipClassPath.xml";

		URL url = getResource(fileNameWithBasePath); //This ClassLoader doesn't have a BasePath so we need to append it
		assertEquals("Path of resource invalid", fileNameWithBasePath, url.getPath());
	}
}
