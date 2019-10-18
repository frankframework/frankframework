package nl.nn.adapterframework.xml;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;
import java.util.jar.JarFile;

import org.apache.xerces.xni.XMLResourceIdentifier;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.classloaders.JarFileClassLoader;
import nl.nn.adapterframework.util.ClassPathEntityResolver;

public class ClassLoaderEntityResolverTest {

	private String publicId="fakePublicId";
	
	@Test
	public void localClassPathFileOnRootOfClasspath() throws SAXException, IOException {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();
		ClassPathEntityResolver resolver = new ClassPathEntityResolver(localClassLoader);

		String systemId="AppConstants.properties"; // this file is known to be in the root of the classpath
		InputSource inputSource = resolver.resolveEntity(publicId, systemId);

		assertNotNull(inputSource);
	}

	@Test
	public void localClassPathFileOnRootOfClasspathAbsolute() throws SAXException, IOException {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();
		ClassPathEntityResolver resolver = new ClassPathEntityResolver(localClassLoader);

		String systemId="/AppConstants.properties"; // this file is known to be in the root of the classpath
		InputSource inputSource = resolver.resolveEntity(publicId, systemId);

		assertNotNull(inputSource);
	}

	@Test
	public void localClassPathAbsolute() throws SAXException, IOException {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();
		ClassPathEntityResolver resolver = new ClassPathEntityResolver(localClassLoader);

		String systemId="/Xslt/importDocument/lookup.xml";
		
		InputSource inputSource = resolver.resolveEntity(publicId, systemId);
		assertNotNull(inputSource);
	}

	
	@Test
	public void bytesClassPath() throws SAXException, IOException, ConfigurationException {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL file = this.getClass().getResource("/classLoader-test.zip");
		assertNotNull("jar url not found", file);
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull("jar file not found",jarFile);

		JarFileClassLoader cl = new JarFileClassLoader(localClassLoader);
		cl.setJar(file.getFile());
		cl.configure(null, "");

		ClassPathEntityResolver resolver = new ClassPathEntityResolver(cl);

		String systemId="/Xslt/names.xsl";
		InputSource inputSource = resolver.resolveEntity(publicId, systemId);
		assertNotNull(inputSource);

	}

	@Test
	public void bytesClassPathAbsolute() throws SAXException, IOException, ConfigurationException  {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL file = this.getClass().getResource("/classLoader-test.zip");
		assertNotNull("jar url not found", file);
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull("jar file not found",jarFile);

		JarFileClassLoader cl = new JarFileClassLoader(localClassLoader);
		cl.setJar(file.getFile());
		cl.configure(null, "");

		ClassPathEntityResolver resolver = new ClassPathEntityResolver(cl);

		String systemId="Xslt/names.xsl";
		InputSource inputSource = resolver.resolveEntity(publicId, systemId);
		assertNotNull(inputSource);
	}

}
