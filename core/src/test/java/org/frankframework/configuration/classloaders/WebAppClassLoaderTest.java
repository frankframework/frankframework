package org.frankframework.configuration.classloaders;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.URL;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class WebAppClassLoaderTest extends ConfigurationClassLoaderTestBase<WebAppClassLoader> {

	@Override
	public WebAppClassLoader createClassLoader(ClassLoader parent) {
		return new WebAppClassLoader(parent);
	}

	@Override
	@Test
	@Disabled
	public void configurationFileDefaultLocation() {
		// Stub this method as the WebAppClassloader always asks its parent for resources, which it does not have.
	}

	@Override
	@Test
	@Disabled
	public void configurationFileCustomLocation() {
		// Stub this method as the WebAppClassloader always asks its parent for resources, which it does not have.
	}

	@Override
	@Test
	@Disabled
	public void configurationFileCustomLocationAndBasePath() {
		// Stub this method as the WebAppClassloader always asks its parent for resources, which it does not have.
	}

	@Test
	@Override
	public void testFolder() throws IOException {
		// Uses a different test folder
		URL folder = getResource("WebAppClassLoader/");
		// Only verify it's exists, we don't care about the contents of the folder for this test as it's a mock.
		assertNotNull(folder);
	}


	private String getAbsoluteFilePath(String name) { // For testing purposes the JAR_FILE archive isn't used, we're just using the path
		URL file = this.getClass().getResource(JAR_FILE);
		assertNotNull(file, "jar url ["+JAR_FILE+"] not found");
		return "jar:file:" + file.getFile() + "!/" + name;
	}

	/* test classpath resource retrieval */
	@Test
	public void absoluteUrl() {
		URL resource = getResource(getAbsoluteFilePath("ClassLoaderTestFile.xml"));
		assertNotNull(resource, "unable to retrieve resource from zip file");
	}

	@Test
	public void absoluteUrlWithoutBasePath() throws Exception {
		createAndConfigure("WebAppClassLoader"); // re-create the classload with basepath

		URL resource = getResource(getAbsoluteFilePath("ClassLoaderTestFile.xml"));
		assertNull(resource); // File is considered illegal and should not be found with this classloader
	}

	@Test
	public void absoluteUrlWithBasePath() throws Exception {
		createAndConfigure("WebAppClassLoader"); // re-create the classload with basepath

		URL resource = getResource(getAbsoluteFilePath("WebAppClassLoader/ClassLoaderTestFile.xml")); // basepath in url!
		assertNotNull(resource, "unable to retrieve resource from zip file");
	}
}
