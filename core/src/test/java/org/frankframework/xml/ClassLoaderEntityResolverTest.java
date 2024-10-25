package org.frankframework.xml;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.frankframework.configuration.classloaders.JarFileClassLoader;
import org.frankframework.core.IScopeProvider;
import org.frankframework.testutil.TestScopeProvider;
import org.frankframework.util.UUIDUtil;

public class ClassLoaderEntityResolverTest {

	private final String publicId = "fakePublicId";
	protected final String JAR_FILE = "/ClassLoader/zip/classLoader-test.zip";
	private final IScopeProvider localScopeProvider = new TestScopeProvider();

	@ParameterizedTest
	@ValueSource(strings = {"AppConstants.properties", "/AppConstants.properties", "/Xslt/importDocument/lookup.xml"})
	public void localClassPathLookup(String systemId) throws SAXException, IOException {
		ClassLoaderEntityResolver resolver = new ClassLoaderEntityResolver(localScopeProvider);

		InputSource inputSource = resolver.resolveEntity(publicId, systemId);

		assertNotNull(inputSource);
	}

	@Test
	public void bytesClassPath() throws Exception {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL file = this.getClass().getResource(JAR_FILE);
		assertNotNull(file, "jar url not found");
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull(jarFile, "jar file not found");

		JarFileClassLoader cl = new JarFileClassLoader(localClassLoader);
		cl.setJar(file.getFile());
		cl.configure(null, "");

		ClassLoaderEntityResolver resolver = new ClassLoaderEntityResolver(TestScopeProvider.wrap(cl));

		String systemId="/ClassLoader/Xslt/names.xsl";
		InputSource inputSource = resolver.resolveEntity(publicId, systemId);
		assertNotNull(inputSource);

	}

	@Test
	public void bytesClassPathAbsolute() throws Exception {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL file = this.getClass().getResource(JAR_FILE);
		assertNotNull(file, "jar url not found");
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull(jarFile, "jar file not found");

		JarFileClassLoader cl = new JarFileClassLoader(localClassLoader);
		cl.setJar(file.getFile());
		cl.configure(null, "");

		ClassLoaderEntityResolver resolver = new ClassLoaderEntityResolver(TestScopeProvider.wrap(cl));

		String systemId="ClassLoader/Xslt/names.xsl";
		InputSource inputSource = resolver.resolveEntity(publicId, systemId);
		assertNotNull(inputSource);
	}

	@Test
	public void testResourceNotFound() throws Exception {
		//Get the working directory
		final URL context = this.getClass().getResource("/");
		assertNotNull(context);

		//Get a random filename
		final String randomName = "myFile-"+ UUIDUtil.createRandomUUID();

		//Find a file which does not exist, but also does not return NULL
		URL file = new URL(context, randomName);
		assertNotNull(file);

		ClassLoaderEntityResolver resolver = new ClassLoaderEntityResolver(localScopeProvider);

		try(InputStream is = file.openStream()) {
			fail("This should fail!"); //Make sure the file cannot be found!
		}
		catch (IOException e) {}
		String systemId = file.getFile(); //Get the full absolute path to the non-existing file

		// Act
		SAXException e = assertThrows(SAXException.class, () -> resolver.resolveEntity(publicId, systemId));
		assertThat(e.getMessage(), containsString("Cannot get resource for publicId [fakePublicId] with systemId "));
	}
}
