/*
   Copyright 2018 Nationale-Nederlanden, 2021 WeAreFrank!

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;

import org.frankframework.testutil.JunitTestClassLoaderWrapper;
import org.frankframework.testutil.TestAppender;
import org.frankframework.util.ClassUtils;

public class JarFileClassLoaderTest extends ConfigurationClassLoaderTestBase<JarFileClassLoader> {

	@Override
	protected String getScheme() {
		return "classpath";
	}

	@Override
	public JarFileClassLoader createClassLoader(ClassLoader parent) throws Exception {
		return createClassLoader(parent, JAR_FILE);
	}

	private JarFileClassLoader createClassLoader(ClassLoader parent, String jarFile) throws Exception {
		URL file = this.getClass().getResource(jarFile);
		assertNotNull(file, "jar url ["+jarFile+"] not found");

		assertNotNull(new JarFile(file.getFile()), "jar file not found"); // verify the jar file

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

		createAndConfigure("ClassLoader"); // Create a ClassLoader with BasePath

		URL url = getResource(filename);
		assertEquals(filename, url.getPath(), "Path of resource invalid");
	}

	@Test
	public void checkFilePathWithBasePath() {
		String fileNameWithBasePath = "ClassLoader/fileOnlyOnZipClassPath.xml";

		URL url = getResource(fileNameWithBasePath); // This ClassLoader doesn't have a BasePath so we need to append it
		assertEquals(fileNameWithBasePath, url.getPath(), "Path of resource invalid");
	}

	@Test
	public void testMyConfig() throws Exception {
		try (TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m").build()) {
			JarFileClassLoader classLoader = createClassLoader(null, "/ClassLoader/zip/myConfig.zip");

			appConstants.put("configurations.myConfig.classLoaderType", classLoader.getClass().getSimpleName());
			classLoader.configure(ibisContext, "myConfig");

			List<String> logEvents = appender.getLogLines();
			URL configurationURL = classLoader.getResource("Configuration.xml");
			assertNotNull(configurationURL, "unable to locate test file [Configuration.xml]");

			assertEquals(9, logEvents.size(), "Should find 9 log messages");
			long warnMsgs = logEvents.stream().filter(k -> k.startsWith("WARN")).count();
			assertEquals(1, warnMsgs, "Should find one warning message");
		}
	}

	@Test
	public void loadCustomClassUsingForName() throws Exception {
		AbstractClassLoader classLoader = createClassLoader(new JunitTestClassLoaderWrapper(), "/ClassLoader/config-jar-with-java-code.jar");
		classLoader.setBasePath(".");
		classLoader.configure(ibisContext, "myConfig");

		classLoader.setAllowCustomClasses(true);
		// native classloading
		Class<?> clazz = Class.forName("org.frankframework.pipes.LargeBlockTester", true, classLoader); // With inner-class
		ClassUtils.newInstance(clazz);

		Field loadedClassesField = AbstractClassLoader.class.getDeclaredField("loadedCustomClasses");
		loadedClassesField.setAccessible(true);
		List<String> loadedCustomClasses = (List<String>) loadedClassesField.get(classLoader);
		assertEquals(3, loadedCustomClasses.size(), "too many classes: "+loadedCustomClasses.toString()); // base + 2 inner classes
		assertTrue(loadedCustomClasses.contains("org.frankframework.pipes.LargeBlockTester"));
	}

	@Test
	public void loadCustomClassUsingLoadClass() throws Exception {
		AbstractClassLoader classLoader = createClassLoader(new JunitTestClassLoaderWrapper(), "/ClassLoader/config-jar-with-java-code.jar");
		classLoader.setBasePath(".");
		classLoader.configure(ibisContext, "myConfig");

		classLoader.setAllowCustomClasses(true);
		// native classloading
		Class<?> clazz = classLoader.loadClass("org.frankframework.pipes.LargeBlockTester"); // With inner-class
		ClassUtils.newInstance(clazz);

		Field loadedClassesField = AbstractClassLoader.class.getDeclaredField("loadedCustomClasses");
		loadedClassesField.setAccessible(true);
		List<String> loadedCustomClasses = (List<String>) loadedClassesField.get(classLoader);
		assertEquals(3, loadedCustomClasses.size(), "too many classes: "+loadedCustomClasses.toString()); // base + 2 inner classes
		assertTrue(loadedCustomClasses.contains("org.frankframework.pipes.LargeBlockTester"));
	}
}
