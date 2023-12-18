package org.frankframework.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarFile;

import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.frankframework.configuration.classloaders.JarFileClassLoader;
import org.frankframework.core.IScopeProvider;
import org.frankframework.testutil.TestScopeProvider;

/**
 * @author Niels Meijer
 * @author Gerrit van Brakel
 */
@TestMethodOrder(MethodName.class)
public class ClassLoaderUtilsTest {

	private String fileName = "Configuration.xml";
	private ClassLoader contextClassLoader = new ContextClassLoader();
	private IScopeProvider scopeProvider = TestScopeProvider.wrap(contextClassLoader);
	private String fileContent = "<test />";

	protected final String JAR_FILE = "/ClassLoader/zip/classLoader-test.zip";
	private IScopeProvider nullScopeProvider = null;

	private static class ContextClassLoader extends ClassLoader {
		public ContextClassLoader() {
			super(Thread.currentThread().getContextClassLoader());
		}
	}

	@Test
	public void getResourceURL() {
		URL baseUrl = contextClassLoader.getResource(fileName);

		assertEquals(baseUrl.getFile(), ClassLoaderUtils.getResourceURL(fileName).getFile());
	}

	@Test
	public void getResourceURLAndValidateContentsO() throws IOException {
		URL url = ClassLoaderUtils.getResourceURL(fileName);
		assertEquals(fileContent, StreamUtil.streamToString(url.openStream()).trim());
	}

	@Test
	public void getResourceURLAndValidateContentsC1() throws IOException {
		URL url = ClassLoaderUtils.getResourceURL(scopeProvider, fileName);
		assertEquals(fileContent, StreamUtil.streamToString(url.openStream()).trim());
	}

	@Test
	public void getResourceURLAndValidateContentsC2() throws IOException {
		URL url = ClassLoaderUtils.getResourceURL(scopeProvider, fileName);
		assertEquals(fileContent, StreamUtil.streamToString(url.openStream()).trim());
	}

	@Test
	public void getResourceURLfromExternalFile() throws URISyntaxException {
		URL url = ClassLoaderUtils.getResourceURL(fileName);
		String fullPath = url.toURI().toString();

		URL url2 = ClassLoaderUtils.getResourceURL(scopeProvider, fullPath, "file");
		assertEquals(url.getFile(), url2.getFile());
	}

	@Test
	public void getResourceURLfromExternalFileError() throws URISyntaxException {
		URL url = ClassLoaderUtils.getResourceURL(fileName);
		String fullPath = url.toURI().toString();

		assertNull(ClassLoaderUtils.getResourceURL(scopeProvider, fullPath, ""));
	}

	@Test
	public void getResourceURLnoFileError() {
		URL url = ClassLoaderUtils.getResourceURL(scopeProvider, "apple.pie.with.raisins");

		assertNull(url);
	}

	@Test
	public void getResourceURLnoExternalFileErrorC1() {
		URL url = ClassLoaderUtils.getResourceURL(scopeProvider, "apple.pie.with.raisins", "file");

		assertNull(url);
	}

	@Test
	public void getResourceURLnoExternalFileErrorC2() {
		URL url = ClassLoaderUtils.getResourceURL(scopeProvider, "file://potato.ext", "file");

		assertEquals("", url.getFile()); //returns an empty string if one does not exist
	}

	@Test
	public void getResourceURLfromHttpProtocolNotAllowed1() {
		URL url = ClassLoaderUtils.getResourceURL(scopeProvider, "http://potato.ext", "");

		assertNull(url);
	}

	@Test
	public void getResourceURLfromHttpProtocolNotAllowed2() {
		URL url = ClassLoaderUtils.getResourceURL(scopeProvider, "http://potato.ext", "file");

		assertNull(url);
	}

	@Test
	public void getResourceURLfromHttpProtocolNotAllowed3() {
		URL url = ClassLoaderUtils.getResourceURL(scopeProvider, "http://potato.ext", "file,thing");

		assertNull(url);
	}

	@Test
	public void getResourceURLfromHttpProtocolNotAllowed4() {
		URL url = ClassLoaderUtils.getResourceURL(scopeProvider, "http://localhost/potato.ext", "file,thing");

		assertNull(url);
	}

	@Test
	public void getResourceURLfromHttpProtocolNotAllowed5() {
		URL url = ClassLoaderUtils.getResourceURL(scopeProvider, "http://localhost/potato.ext", "https");

		assertNull(url);
	}

	@Test
	public void getResourceURLfromHttpProtocolAllowed1() {
		URL url = ClassLoaderUtils.getResourceURL(scopeProvider, "http://localhost/potato.ext", "http");

		assertNotNull(url);
	}

	@Test
	public void getResourceURLnullClassLoader1() throws IOException {
		URL url = ClassLoaderUtils.getResourceURL(nullScopeProvider, fileName);
		assertEquals(fileContent, StreamUtil.streamToString(url.openStream()).trim());
	}

	@Test
	public void getResourceURLnullClassLoaderNonExistingFile() {
		URL url = ClassLoaderUtils.getResourceURL(nullScopeProvider, "apple.pie.with.raisins");

		assertNull(url);
	}

	@Test
	public void getResourceURLnullClassLoaderWithExternalFile() {
		URL url = ClassLoaderUtils.getResourceURL(nullScopeProvider, "file://potato.ext", "file");

		assertNotNull(url);
		assertEquals("", url.getFile()); //returns an empty string if one does not exist
	}


	public void testUri(IScopeProvider cl, String uri, String expected, String allowedProtocol) throws IOException  {
		URL url = ClassLoaderUtils.getResourceURL(cl, uri, allowedProtocol);
		verifyUrl(url, uri, expected);
	}

	public void testUri(IScopeProvider cl, String uri, String expected) throws IOException  {
		URL url = ClassLoaderUtils.getResourceURL(cl, uri);
		verifyUrl(url, uri, expected);
	}

	public void verifyUrl(URL url, String uri, String expected) throws IOException  {
		assertNotNull(url, "URL for ["+uri+"] should not be null");

		if (expected!=null) {
			assertEquals(expected, StreamUtil.streamToString(url.openStream()));
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
	public void localClassLoader5UrlWithFileScheme() throws Exception {
		String resource="/ClassLoader/ClassLoaderTestFile";
		URL url = ClassLoaderUtils.getResourceURL(scopeProvider, resource);
		String resourceAsFileUrl=url.toExternalForm();
		assertThat(resourceAsFileUrl, startsWith("file:"));

		testUri(scopeProvider, resourceAsFileUrl,"-- /ClassLoader/ClassLoaderTestFile --","file");
	}

	@Test
	public void localClassLoader6Overrideable() throws IOException {
		testUri(scopeProvider, "/ClassLoader/overridablefile","local:/overrideablefile");
	}

	@Test
	public void localClassLoader6UrlWithFileSchemeButNotAllowed() {
		String resource="/ClassLoader/ClassLoaderTestFile";
		URL url = ClassLoaderUtils.getResourceURL(scopeProvider, resource);
		String resourceAsFileUrl=url.toExternalForm();
		assertThat(resourceAsFileUrl, startsWith("file:"));

		URL actual = ClassLoaderUtils.getResourceURL(scopeProvider, resourceAsFileUrl);
		assertNull(actual, "file protocol was allowed but should not");
	}

	@Test
	public void bytesClassLoader01Root() throws Exception {
		testUri(getBytesClassLoaderProvider(), "/ClassLoaderTestFile","-- /ClassLoaderTestFile --");
	}
	@Test
	public void bytesClassLoader02RootNoSlash() throws Exception {
		testUri(getBytesClassLoaderProvider(), "ClassLoaderTestFile","-- /ClassLoaderTestFile --");
	}

	@Test
	public void bytesClassLoader03Folder() throws Exception {
		testUri(getBytesClassLoaderProvider(), "/ClassLoader/ClassLoaderTestFile","-- /ClassLoader/ClassLoaderTestFile --");
	}
	@Test
	public void bytesClassLoader04FolderNoSlash() throws Exception {
		testUri(getBytesClassLoaderProvider(), "ClassLoader/ClassLoaderTestFile","-- /ClassLoader/ClassLoaderTestFile --");
	}

	@Test
	public void bytesClassLoader05ResourceFromLocalClasspath() throws Exception {
		testUri(getBytesClassLoaderProvider(), "/ClassLoader/fileOnlyOnLocalClassPath.txt","-- /ClassLoader/fileOnlyOnLocalClassPath.txt --");
	}

	@Test
	public void bytesClassLoader06ResourceFromLocalClasspathNoSlash() throws Exception {
		testUri(getBytesClassLoaderProvider(), "ClassLoader/fileOnlyOnLocalClassPath.txt","-- /ClassLoader/fileOnlyOnLocalClassPath.txt --");
	}

	@Test
	public void bytesClassLoader07Overridable() throws Exception {
		testUri(getBytesClassLoaderProvider(), "/ClassLoader/overridablefile","zip:/overrideablefile");
	}


	@Test
	public void bytesClassLoader07WithScheme() throws Exception {
		testUri(getBytesClassLoaderProvider(), "classpath:/ClassLoader/ClassLoaderTestFile","-- /ClassLoader/ClassLoaderTestFile --");
	}
	@Test
	public void bytesClassLoader08WithSchemeNoSlash() throws Exception {
		testUri(getBytesClassLoaderProvider(), "classpath:ClassLoader/ClassLoaderTestFile","-- /ClassLoader/ClassLoaderTestFile --");
	}

	@Test
	public void bytesClassLoader09UrlWithFileScheme() throws Exception {
		String resource="/ClassLoader/fileOnlyOnLocalClassPath.txt";
		URL url = ClassLoaderUtils.getResourceURL(scopeProvider, resource);
		String resourceAsFileUrl=url.toExternalForm();
		assertThat(resourceAsFileUrl, startsWith("file:"));

		testUri(getBytesClassLoaderProvider(), resourceAsFileUrl,"-- /ClassLoader/fileOnlyOnLocalClassPath.txt --","file");
	}

	@Test
	public void bytesClassLoader10UrlWithFileSchemeButNotAllowed() throws Exception {
		String resource="/ClassLoader/fileOnlyOnLocalClassPath.xml";
		URL url = ClassLoaderUtils.getResourceURL(scopeProvider, resource);
		String resourceAsFileUrl=url.toExternalForm();
		assertThat(resourceAsFileUrl, startsWith("file:"));

		URL actual = ClassLoaderUtils.getResourceURL(getBytesClassLoaderProvider(), resourceAsFileUrl);
		assertNull(actual, "file protocol was allowed but should not");
	}

	private IScopeProvider getBytesClassLoaderProvider() throws Exception {

		URL file = this.getClass().getResource(JAR_FILE);
		assertNotNull(file, "jar url ["+JAR_FILE+"] not found");
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull(jarFile, "jar file not found");

		JarFileClassLoader cl = new JarFileClassLoader(new ContextClassLoader());
		cl.setJar(file.getFile());
		cl.setBasePath(".");
		cl.configure(null, "");
		return TestScopeProvider.wrap(cl);
	}
}
