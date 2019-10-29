package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarFile;

import static org.hamcrest.core.StringStartsWith.*;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.classloaders.JarFileClassLoader;

/**
 * @author Niels Meijer
 * @author Gerrit van Brakel
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClassUtilsTest {

	private String fileName = "Configuration.xml";
	private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	private String fileContent = "<test />";

	protected final String JAR_FILE = "/Classloader/zip/classLoader-test.zip";

	@Test
	public void getResourceURL() throws URISyntaxException, IOException {
		URL baseUrl = classLoader.getResource(fileName);

		assertEquals(baseUrl.getFile(), ClassUtils.getResourceURL(this, fileName).getFile());
	}

	@Test
	public void getResourceURLAndValidateContentsO() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(this, fileName);
		assertEquals(fileContent, Misc.streamToString(url.openStream()).trim());
	}

	@Test
	public void getResourceURLAndValidateContentsC1() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(classLoader, fileName);
		assertEquals(fileContent, Misc.streamToString(url.openStream()).trim());
	}

	@Test
	public void getResourceURLAndValidateContentsC2() throws URISyntaxException, IOException {
		ClassLoader classLoader = this.getClass().getClassLoader();
		URL url = ClassUtils.getResourceURL(classLoader, fileName);
		assertEquals(fileContent, Misc.streamToString(url.openStream()).trim());
	}

	@Test
	public void getResourceURLfromExternalFile() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(this, fileName);
		String fullPath = url.toURI().toString();

		URL url2 = ClassUtils.getResourceURL(classLoader, fullPath, "file");
		assertEquals(url.getFile(), url2.getFile());
	}

	@Test
	public void getResourceURLfromExternalFileError() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(this, fileName);
		String fullPath = url.toURI().toString();

		assertNull(ClassUtils.getResourceURL(classLoader, fullPath, ""));
	}

	@Test
	public void getResourceURLnoFileError() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(classLoader, "apple.pie.with.raisins");

		assertNull(url);
	}

	@Test
	public void getResourceURLnoExternalFileErrorC1() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(classLoader, "apple.pie.with.raisins", "file");

		assertNull(url);
	}

	@Test
	public void getResourceURLnoExternalFileErrorC2() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(classLoader, "file://potato.ext", "file");

		assertEquals("", url.getFile()); //returns an empty string if one does not exist
	}

	@Test
	public void getResourceURLfromHttpProtocolNotAllowed1() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(classLoader, "http://potato.ext", "");

		assertNull(url);
	}

	@Test
	public void getResourceURLfromHttpProtocolNotAllowed2() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(classLoader, "http://potato.ext", "file");

		assertNull(url);
	}

	@Test
	public void getResourceURLfromHttpProtocolNotAllowed3() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(classLoader, "http://potato.ext", "file,thing");

		assertNull(url);
	}

	@Test
	public void getResourceURLfromHttpProtocolNotAllowed4() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(classLoader, "http://localhost/potato.ext", "file,thing");

		assertNull(url);
	}

	@Test
	public void getResourceURLfromHttpProtocolNotAllowed5() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(classLoader, "http://localhost/potato.ext", "https");

		assertNull(url);
	}

	@Test
	public void getResourceURLfromHttpProtocolAllowed1() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(classLoader, "http://localhost/potato.ext", "http");

		assertNotNull(url);
	}

	@Test
	public void getResourceURLnullClassLoader1() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(null, fileName);
		assertEquals(fileContent, Misc.streamToString(url.openStream()).trim());
	}

	@Test
	public void getResourceURLnullClassLoaderNonExistingFile() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(null, "apple.pie.with.raisins");

		assertNull(url);
	}

	@Test
	public void getResourceURLnullClassLoaderWithExternalFile() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(null, "file://potato.ext", "file");

		assertEquals("", url.getFile()); //returns an empty string if one does not exist
	}
	
	
	
	
	public void testUri(ClassLoader cl, String uri, String expected, String allowedProtocol) throws IOException  {
		URL url = ClassUtils.getResourceURL(cl, uri, allowedProtocol);
		verifyUrl(url, uri, expected);
	}
	
	public void testUri(ClassLoader cl, String uri, String expected) throws IOException  {
		URL url = ClassUtils.getResourceURL(cl, uri);
		verifyUrl(url, uri, expected);
	}
	
	public void verifyUrl(URL url, String uri, String expected) throws IOException  {
		assertNotNull("URL for ["+uri+"] should not be null",url);
		
		if (expected!=null) {
			assertEquals(expected, Misc.streamToString(url.openStream()));
		}
	}
	

	@Test
	public void localClassLoader1() throws IOException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		testUri(cl,"/ClassLoader/folder/file.xml","<file>file:/folder/file.xml</file>");
	}

	@Test
	public void localClassLoader2NoSlash() throws IOException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		testUri(cl,"ClassLoader/folder/file.xml","<file>file:/folder/file.xml</file>");
	}

	@Test
	public void localClassLoader2UrlWithFileScheme() throws IOException, ConfigurationException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();

		String resource="/ClassLoader/folder/file.xml";
		URL url = ClassUtils.getResourceURL(cl, resource);
		String resourceAsFileUrl=url.toExternalForm();
		assertThat(resourceAsFileUrl, startsWith("file:"));

		testUri(cl,resourceAsFileUrl,"<file>file:/folder/file.xml</file>","file");
	}

	@Test
	public void localClassLoader3UrlWithFileSchemeButNotAllowed() throws IOException, ConfigurationException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();

		String resource="/ClassLoader/folder/file.xml";
		URL url = ClassUtils.getResourceURL(cl, resource);
		String resourceAsFileUrl=url.toExternalForm();
		assertThat(resourceAsFileUrl, startsWith("file:"));

		URL actual = ClassUtils.getResourceURL(cl, resourceAsFileUrl);
		assertNull("file protocol was allowed but should not", actual);
	}

	@Test
	public void bytesClassLoader1() throws IOException, ConfigurationException {
		ClassLoader cl = getBytesClassLoader();
		testUri(cl,"/ClassLoader/folder/file.xml","<file>zip:/folder/file.xml</file>");
	}
	@Test
	public void bytesClassLoader2NoSlash() throws IOException, ConfigurationException {
		ClassLoader cl = getBytesClassLoader();
		testUri(cl,"ClassLoader/folder/file.xml","<file>zip:/folder/file.xml</file>");
	}

	@Test
	public void bytesClassLoader3ResourceFromLocalClasspath() throws IOException, ConfigurationException {
		ClassLoader cl = getBytesClassLoader();
		testUri(cl,"/ClassLoader/folder/fileOnlyOnLocalClassPath.xml","<file>file:/folder/fileOnlyOnLocalClassPath.xml</file>");
	}

	@Test
	public void bytesClassLoader4ResourceFromLocalClasspathNoSlash() throws IOException, ConfigurationException {
		ClassLoader cl = getBytesClassLoader();
		testUri(cl,"ClassLoader/folder/fileOnlyOnLocalClassPath.xml","<file>file:/folder/fileOnlyOnLocalClassPath.xml</file>");
	}

	@Test
	public void bytesClassLoader5WithScheme() throws IOException, ConfigurationException {
		ClassLoader cl = getBytesClassLoader();
		testUri(cl,"bytesclassloader:/ClassLoader/folder/file.xml","<file>zip:/folder/file.xml</file>");
	}
	@Test
	public void bytesClassLoader6WithSchemeNoSlash() throws IOException, ConfigurationException {
		ClassLoader cl = getBytesClassLoader();
		testUri(cl,"bytesclassloader:ClassLoader/folder/file.xml","<file>zip:/folder/file.xml</file>");
	}

	@Test
	public void bytesClassLoader7UrlWithFileScheme() throws IOException, ConfigurationException {
		ClassLoader clres = Thread.currentThread().getContextClassLoader();

		String resource="/ClassLoader/folder/fileOnlyOnLocalClassPath.xml";
		URL url = ClassUtils.getResourceURL(clres, resource);
		String resourceAsFileUrl=url.toExternalForm();
		assertThat(resourceAsFileUrl, startsWith("file:"));

		ClassLoader cl = getBytesClassLoader();
		testUri(cl,resourceAsFileUrl,"<file>file:/folder/fileOnlyOnLocalClassPath.xml</file>","file");
	}

	@Test
	public void bytesClassLoader8UrlWithFileSchemeButNotAllowed() throws IOException, ConfigurationException {
		ClassLoader clres = Thread.currentThread().getContextClassLoader();

		String resource="/ClassLoader/folder/fileOnlyOnLocalClassPath.xml";
		URL url = ClassUtils.getResourceURL(clres, resource);
		String resourceAsFileUrl=url.toExternalForm();
		assertThat(resourceAsFileUrl, startsWith("file:"));
		ClassLoader cl = getBytesClassLoader();

		URL actual = ClassUtils.getResourceURL(cl, resourceAsFileUrl);
		assertNull("file protocol was allowed but should not", actual);
	}

	private ClassLoader getBytesClassLoader() throws IOException, ConfigurationException {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL file = this.getClass().getResource(JAR_FILE);
		assertNotNull("jar url ["+JAR_FILE+"] not found", file);
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull("jar file not found",jarFile);

		JarFileClassLoader cl = new JarFileClassLoader(localClassLoader);
		cl.setJar(file.getFile());
		cl.configure(null, "");
		return cl;
	}

}
