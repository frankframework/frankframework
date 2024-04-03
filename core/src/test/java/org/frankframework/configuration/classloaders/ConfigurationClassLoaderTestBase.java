/*
   Copyright 2018, 2019 Nationale-Nederlanden

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;

import org.frankframework.configuration.ApplicationWarnings;
import org.frankframework.configuration.ConfigurationUtils;
import org.frankframework.configuration.IbisContext;
import org.frankframework.configuration.classloaders.IConfigurationClassLoader.ReportLevel;
import org.frankframework.util.AppConstants;
import org.frankframework.util.UUIDUtil;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public abstract class ConfigurationClassLoaderTestBase<C extends ClassLoaderBase> extends Mockito {

	protected final String JAR_FILE = "/ClassLoader/zip/classLoader-test.zip";

	private ClassLoaderBase classLoader = null;
	protected IbisContext ibisContext = spy(new IbisContext());
	protected String scheme = "file";
	protected AppConstants appConstants;
	private String configurationName = null;

	abstract C createClassLoader(ClassLoader parent) throws Exception;

	@BeforeEach
	public void setUp() throws Exception {
		appConstants = AppConstants.getInstance();
		createAndConfigure(".");

		ApplicationWarnings.removeInstance();
	}

	protected void createAndConfigure() throws Exception {
		createAndConfigure(null);
	}
	protected void createAndConfigure(String basePath) throws Exception {
		ClassLoader parent = new ClassLoaderMock();
		classLoader = createClassLoader(parent);

		if(basePath != null) {
			classLoader.setBasePath(basePath);
		}

		appConstants.put("configurations."+getConfigurationName()+".classLoaderType", classLoader.getClass().getSimpleName());
		classLoader.configure(ibisContext, getConfigurationName());
	}

	/**
	 * Returns the scheme, defaults to <code>file</code>
	 * @return scheme to test against
	 */
	protected String getScheme() {
		return scheme;
	}

	/**
	 * Returns a dummy configuration name
	 * @return name of the configuration
	 */
	protected String getConfigurationName() {
		if(configurationName == null)
			configurationName = "dummyConfigurationName"+ UUIDUtil.createRandomUUID();

		return configurationName;
	}

	public URL getResource(String resource) {
		return classLoader.getResource(resource);
	}

	public void resourceExists(String resource) {
		resourceExists(resource, getScheme());
	}
	public void resourceExists(String resource, String scheme) {
		URL url = getResource(resource);
		assertNotNull(url, "cannot find resource");
		String file = url.toString();
		assertTrue(file.startsWith(scheme + ":"), "scheme["+scheme+"] is wrong for file["+file+"]");
		assertTrue(file.endsWith(resource), "name is wrong");
	}

	public void resourcesExists(String name) throws IOException {
		resourcesExists(name, getScheme());
	}
	public void resourcesExists(String name, String scheme) throws IOException {
		LinkedList<String> schemes = new LinkedList<>();
		if(!"scheme".equals(scheme))
			schemes.add(scheme);
		schemes.add("file");
		resourcesExists(name, schemes);
	}

	/**
	 * In which resources are retrieved matters!
	 * @param name
	 * @param schemes
	 * @throws IOException
	 */
	public void resourcesExists(String name, LinkedList<String> schemes) throws IOException {
		Enumeration<URL> resources = classLoader.getResources(name);
		while(resources.hasMoreElements()) {
			URL url = resources.nextElement();
			assertNotNull(url, "cannot find resource");
			String file = url.toString();
			String scheme = schemes.removeFirst();
			assertTrue(file.startsWith(scheme + ":"), "scheme["+scheme+"] is wrong for file["+file+"]");
			assertTrue(file.endsWith(name), "name is wrong");
		}
	}

	@Test
	public void fileNotFound() {
		assertNull(getResource("not-found.txt"));
	}

	/* getResource() */
	@Test
	public void testFile() {
		resourceExists("ClassLoaderTestFile");
	}

	@Test
	public void testFileTxt() {
		resourceExists("ClassLoaderTestFile.txt");
	}

	@Test
	public void textFileXml() {
		resourceExists("ClassLoaderTestFile.xml");
	}

	@Test
	public void textFolderFile() {
		resourceExists("ClassLoader/ClassLoaderTestFile");
	}

	@Test
	public void textFolderFileTxt() {
		resourceExists("ClassLoader/ClassLoaderTestFile.txt");
	}

	@Test
	public void textFolderFileXml() {
		resourceExists("ClassLoader/ClassLoaderTestFile.xml");
	}

	@Test
	public void parentOnlyFile() {
		resourceExists("parent_only.xml", "file");
	}

	@Test
	public void parentOnlyFolder() {
		resourceExists("folder/parent_only.xml", "file");
	}

	//Not only test through setters and getters but also properties
	@Test
	public void testSchemeWithClassLoaderManager() {
		URL resource = getResource("ClassLoaderTestFile.xml");

		assertNotNull(resource, "resource ["+resource+"] must be found");
		assertTrue(resource.toString().startsWith(getScheme()),"resource ["+resource+"] must start with scheme ["+getScheme()+"]");
		assertTrue(resource.toString().endsWith("ClassLoaderTestFile.xml"), "resource ["+resource+"] must end with [ClassLoaderTestFile.xml]");
	}

	// make sure default level is always error
	@Test
	public void testReportLevelERROR() {
		classLoader.setReportLevel("dummy");
		assertTrue(classLoader.getReportLevel() == ReportLevel.ERROR);
	}

	// test lowercase level
	@Test
	public void testReportLeveldebug() {
		classLoader.setReportLevel("debug");
		assertTrue(classLoader.getReportLevel() == ReportLevel.DEBUG);
	}

	// test uppercase level
	@Test
	public void testReportLevelDEBUG() {
		classLoader.setReportLevel("DEBUG");
		assertTrue(classLoader.getReportLevel() == ReportLevel.DEBUG);
	}

	@Test
	public void configurationFileDefaultLocation() {
		String configFile = ConfigurationUtils.getConfigurationFile(classLoader, getConfigurationName());
		assertEquals("Configuration.xml", configFile);
		URL configURL = classLoader.getResource(configFile);
		assertNotNull(configURL, "config file ["+configFile+"] cannot be found");
		assertTrue(configURL.toString().endsWith(configFile));
	}

	@Test
	public void configurationFileCustomLocation() throws Exception {
		createAndConfigure("Config");
		String name = "Config/NonDefaultConfiguration.xml";
		AppConstants.getInstance(classLoader).put("configurations."+getConfigurationName()+".configurationFile", name);
		String configFile = ConfigurationUtils.getConfigurationFile(classLoader, getConfigurationName());
		assertEquals("NonDefaultConfiguration.xml", configFile);
		URL configURL = classLoader.getResource(configFile);
		assertNotNull(configURL, "config file ["+configFile+"] cannot be found");
		assertTrue(configURL.toString().endsWith(configFile));
	}

	@Test
	public void configurationFileCustomLocationAndBasePath() throws Exception {
		String file = "NonDefaultConfiguration.xml";
		String basePath = "Config";
		String path = basePath + "/" + file;

		//Order is everything!
		ClassLoader parent = new ClassLoaderMock();
		appConstants = AppConstants.getInstance();
		classLoader = createClassLoader(parent);

//		classLoader.setBasePath(basePath);

		// We have to set both the name as well as the appconstants variable.
		String configKey = "configurations."+getConfigurationName()+".configurationFile";
		AppConstants.getInstance(classLoader).put(configKey, file);
		classLoader.setConfigurationFile(path);

		appConstants.put("configurations."+getConfigurationName()+".classLoaderType", classLoader.getClass().getSimpleName());
		classLoader.configure(ibisContext, getConfigurationName());

		String configFile = ConfigurationUtils.getConfigurationFile(classLoader, getConfigurationName());
		assertEquals(file, configFile, "configurationFile path does not match");
		URL configURL = classLoader.getResource(configFile);
		assertNotNull(configURL, "config file ["+configFile+"] cannot be found");
		assertTrue(configURL.getPath().endsWith(file));
	}

	@Test
	public void toStringTest() {
		String logPrefix = classLoader.getClass().getSimpleName() + "@" + Integer.toHexString(classLoader.hashCode());

		//Should match DatabaseClassLoader@1234abcd[<CONFIG-NAME>]
		assertThat(classLoader.toString(), Matchers.startsWith(logPrefix+"["+getConfigurationName()+"]"));
	}

	@Test
	public void testInvalidPath() {
		URL url = getResource("//foo/../bar.txt");
		assertNull(url);
	}
}
