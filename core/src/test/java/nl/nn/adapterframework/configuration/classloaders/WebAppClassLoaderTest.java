package nl.nn.adapterframework.configuration.classloaders;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URL;

import org.junit.jupiter.api.Test;

public class WebAppClassLoaderTest extends ConfigurationClassLoaderTestBase<WebAppClassLoader> {

	@Override
	public WebAppClassLoader createClassLoader(ClassLoader parent) throws Exception {
		return new WebAppClassLoader(parent);
	}

	@Override
	public void configurationFileDefaultLocation() {
		//Stub this method as the WebAppClassloader always asks it's parent for resources, which it does not have.
	}

	@Override
	public void configurationFileCustomLocation() throws Exception {
		//Stub this method as the WebAppClassloader always asks it's parent for resources, which it does not have.
	}

	@Override
	public void configurationFileCustomLocationAndBasePath() throws Exception {
		//Stub this method as the WebAppClassloader always asks it's parent for resources, which it does not have.
	}

	private String getAbsoluteFilePath(String name) { //For testing purposes the JAR_FILE archive isn't used, we're just using the path
		URL file = this.getClass().getResource(JAR_FILE);
		assertNotNull(file, "jar url ["+JAR_FILE+"] not found");
		return "jar:file:" + file.getFile() + "!/" + name;
	}

	/* test classpath resource retrieval */
	@Test
	public void absoluteUrl() throws Exception {
		URL resource = getResource(getAbsoluteFilePath("ClassLoaderTestFile.xml"));
		assertNotNull(resource, "unable to retrieve resource from zip file");
	}

	@Test
	public void absoluteUrlWithoutBasePath() throws Exception {
		createAndConfigure("WebAppClassLoader"); //re-create the classload with basepath

		URL resource = getResource(getAbsoluteFilePath("ClassLoaderTestFile.xml"));
		assertNull(resource); //File is considered illegal and should not be found with this classloader
	}

	@Test
	public void absoluteUrlWithBasePath() throws Exception {
		createAndConfigure("WebAppClassLoader"); //re-create the classload with basepath

		URL resource = getResource(getAbsoluteFilePath("WebAppClassLoader/ClassLoaderTestFile.xml")); //basepath in url!
		assertNotNull(resource, "unable to retrieve resource from zip file");
	}
}
