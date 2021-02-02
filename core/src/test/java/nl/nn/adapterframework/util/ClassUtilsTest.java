package nl.nn.adapterframework.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarFile;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.classloaders.JarFileClassLoader;
import nl.nn.adapterframework.core.IScopeProvider;
import nl.nn.adapterframework.testutil.TestScopeProvider;

/**
 * @author Niels Meijer
 * @author Gerrit van Brakel
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClassUtilsTest {

	private String fileName = "Configuration.xml";
	private ClassLoader contextClassLoader = new ContextClassLoader();
	private IScopeProvider scopeProvider = TestScopeProvider.wrap(contextClassLoader);
	private String fileContent = "<test />";

	private static class ContextClassLoader extends ClassLoader {
		public ContextClassLoader() {
			super(Thread.currentThread().getContextClassLoader());
		}
	}

	protected final String JAR_FILE = "/ClassLoader/zip/classLoader-test.zip";
	private IScopeProvider nullScopeProvider = null;

	@Test
	public void getResourceURL() throws URISyntaxException, IOException {
		URL baseUrl = contextClassLoader.getResource(fileName);

		assertEquals(baseUrl.getFile(), ClassUtils.getResourceURL(fileName).getFile());
	}

	@Test
	public void getResourceURLAndValidateContentsO() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(fileName);
		assertEquals(fileContent, Misc.streamToString(url.openStream()).trim());
	}

	@Test
	public void getResourceURLAndValidateContentsC1() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(scopeProvider, fileName);
		assertEquals(fileContent, Misc.streamToString(url.openStream()).trim());
	}

	@Test
	public void getResourceURLAndValidateContentsC2() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(scopeProvider, fileName);
		assertEquals(fileContent, Misc.streamToString(url.openStream()).trim());
	}

	@Test
	public void getResourceURLfromExternalFile() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(fileName);
		String fullPath = url.toURI().toString();

		URL url2 = ClassUtils.getResourceURL(scopeProvider, fullPath, "file");
		assertEquals(url.getFile(), url2.getFile());
	}

	@Test
	public void getResourceURLfromExternalFileError() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(fileName);
		String fullPath = url.toURI().toString();

		assertNull(ClassUtils.getResourceURL(scopeProvider, fullPath, ""));
	}

	@Test
	public void getResourceURLnoFileError() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(scopeProvider, "apple.pie.with.raisins");

		assertNull(url);
	}

	@Test
	public void getResourceURLnoExternalFileErrorC1() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(scopeProvider, "apple.pie.with.raisins", "file");

		assertNull(url);
	}

	@Test
	public void getResourceURLnoExternalFileErrorC2() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(scopeProvider, "file://potato.ext", "file");

		assertEquals("", url.getFile()); //returns an empty string if one does not exist
	}

	@Test
	public void getResourceURLfromHttpProtocolNotAllowed1() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(scopeProvider, "http://potato.ext", "");

		assertNull(url);
	}

	@Test
	public void getResourceURLfromHttpProtocolNotAllowed2() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(scopeProvider, "http://potato.ext", "file");

		assertNull(url);
	}

	@Test
	public void getResourceURLfromHttpProtocolNotAllowed3() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(scopeProvider, "http://potato.ext", "file,thing");

		assertNull(url);
	}

	@Test
	public void getResourceURLfromHttpProtocolNotAllowed4() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(scopeProvider, "http://localhost/potato.ext", "file,thing");

		assertNull(url);
	}

	@Test
	public void getResourceURLfromHttpProtocolNotAllowed5() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(scopeProvider, "http://localhost/potato.ext", "https");

		assertNull(url);
	}

	@Test
	public void getResourceURLfromHttpProtocolAllowed1() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(scopeProvider, "http://localhost/potato.ext", "http");

		assertNotNull(url);
	}

	@Test
	public void getResourceURLnullClassLoader1() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(nullScopeProvider, fileName);
		assertEquals(fileContent, Misc.streamToString(url.openStream()).trim());
	}

	@Test
	public void getResourceURLnullClassLoaderNonExistingFile() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(nullScopeProvider, "apple.pie.with.raisins");

		assertNull(url);
	}

	@Test
	public void getResourceURLnullClassLoaderWithExternalFile() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(nullScopeProvider, "file://potato.ext", "file");

		assertNotNull(url);
		assertEquals("", url.getFile()); //returns an empty string if one does not exist
	}


	public void testUri(IScopeProvider cl, String uri, String expected, String allowedProtocol) throws IOException  {
		URL url = ClassUtils.getResourceURL(cl, uri, allowedProtocol);
		verifyUrl(url, uri, expected);
	}

	public void testUri(IScopeProvider cl, String uri, String expected) throws IOException  {
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
	public void localClassLoader1FromRoot() throws IOException {
		testUri(scopeProvider, "/ClassLoaderTestFile","-- /ClassLoaderTestFile --");
	}

	@Test
	public void localClassLoader2FromRootNoSlash() throws IOException {
		testUri(scopeProvider, "ClassLoaderTestFile","-- /ClassLoaderTestFile --");
	}

	@Test
	public void localClassLoader3FromFolder() throws IOException {
		testUri(scopeProvider, "/ClassLoader/ClassLoaderTestFile","-- /ClassLoader/ClassLoaderTestFile --");
	}

	@Test
	public void localClassLoader4FromFolderNoSlash() throws IOException {
		testUri(scopeProvider,"ClassLoader/ClassLoaderTestFile","-- /ClassLoader/ClassLoaderTestFile --");
	}

	@Test
	public void localClassLoader5UrlWithFileScheme() throws IOException, ConfigurationException {
		String resource="/ClassLoader/ClassLoaderTestFile";
		URL url = ClassUtils.getResourceURL(scopeProvider, resource);
		String resourceAsFileUrl=url.toExternalForm();
		assertThat(resourceAsFileUrl, startsWith("file:"));

		testUri(scopeProvider, resourceAsFileUrl,"-- /ClassLoader/ClassLoaderTestFile --","file");
	}

	@Test
	public void localClassLoader6Overrideable() throws IOException {
		testUri(scopeProvider, "/ClassLoader/overridablefile","local:/overrideablefile");
	}

	@Test
	public void localClassLoader6UrlWithFileSchemeButNotAllowed() throws IOException, ConfigurationException {
		String resource="/ClassLoader/ClassLoaderTestFile";
		URL url = ClassUtils.getResourceURL(scopeProvider, resource);
		String resourceAsFileUrl=url.toExternalForm();
		assertThat(resourceAsFileUrl, startsWith("file:"));

		URL actual = ClassUtils.getResourceURL(scopeProvider, resourceAsFileUrl);
		assertNull("file protocol was allowed but should not", actual);
	}

	@Test
	public void bytesClassLoader01Root() throws IOException, ConfigurationException {
		testUri(getBytesClassLoaderProvider(), "/ClassLoaderTestFile","-- /ClassLoaderTestFile --");
	}
	@Test
	public void bytesClassLoader02RootNoSlash() throws IOException, ConfigurationException {
		testUri(getBytesClassLoaderProvider(), "ClassLoaderTestFile","-- /ClassLoaderTestFile --");
	}

	@Test
	public void bytesClassLoader03Folder() throws IOException, ConfigurationException {
		testUri(getBytesClassLoaderProvider(), "/ClassLoader/ClassLoaderTestFile","-- /ClassLoader/ClassLoaderTestFile --");
	}
	@Test
	public void bytesClassLoader04FolderNoSlash() throws IOException, ConfigurationException {
		testUri(getBytesClassLoaderProvider(), "ClassLoader/ClassLoaderTestFile","-- /ClassLoader/ClassLoaderTestFile --");
	}

	@Test
	public void bytesClassLoader05ResourceFromLocalClasspath() throws IOException, ConfigurationException {
		testUri(getBytesClassLoaderProvider(), "/ClassLoader/fileOnlyOnLocalClassPath.txt","-- /ClassLoader/fileOnlyOnLocalClassPath.txt --");
	}

	@Test
	public void bytesClassLoader06ResourceFromLocalClasspathNoSlash() throws IOException, ConfigurationException {
		testUri(getBytesClassLoaderProvider(), "ClassLoader/fileOnlyOnLocalClassPath.txt","-- /ClassLoader/fileOnlyOnLocalClassPath.txt --");
	}

	@Test
	public void bytesClassLoader07Overridable() throws IOException, ConfigurationException {
		testUri(getBytesClassLoaderProvider(), "/ClassLoader/overridablefile","zip:/overrideablefile");
	}


	@Test
	public void bytesClassLoader07WithScheme() throws IOException, ConfigurationException {
		testUri(getBytesClassLoaderProvider(), "classpath:/ClassLoader/ClassLoaderTestFile","-- /ClassLoader/ClassLoaderTestFile --");
	}
	@Test
	public void bytesClassLoader08WithSchemeNoSlash() throws IOException, ConfigurationException {
		testUri(getBytesClassLoaderProvider(), "classpath:ClassLoader/ClassLoaderTestFile","-- /ClassLoader/ClassLoaderTestFile --");
	}

	@Test
	public void bytesClassLoader09UrlWithFileScheme() throws IOException, ConfigurationException {
		String resource="/ClassLoader/fileOnlyOnLocalClassPath.txt";
		URL url = ClassUtils.getResourceURL(scopeProvider, resource);
		String resourceAsFileUrl=url.toExternalForm();
		assertThat(resourceAsFileUrl, startsWith("file:"));

		testUri(getBytesClassLoaderProvider(), resourceAsFileUrl,"-- /ClassLoader/fileOnlyOnLocalClassPath.txt --","file");
	}

	@Test
	public void bytesClassLoader10UrlWithFileSchemeButNotAllowed() throws IOException, ConfigurationException {
		String resource="/ClassLoader/fileOnlyOnLocalClassPath.xml";
		URL url = ClassUtils.getResourceURL(scopeProvider, resource);
		String resourceAsFileUrl=url.toExternalForm();
		assertThat(resourceAsFileUrl, startsWith("file:"));

		URL actual = ClassUtils.getResourceURL(getBytesClassLoaderProvider(), resourceAsFileUrl);
		assertNull("file protocol was allowed but should not", actual);
	}

	private IScopeProvider getBytesClassLoaderProvider() throws IOException, ConfigurationException {

		URL file = this.getClass().getResource(JAR_FILE);
		assertNotNull("jar url ["+JAR_FILE+"] not found", file);
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull("jar file not found",jarFile);

		JarFileClassLoader cl = new JarFileClassLoader(new ContextClassLoader());
		cl.setJar(file.getFile());
		cl.setBasePath(".");
		cl.configure(null, "");
		return TestScopeProvider.wrap(cl);
	}

}
