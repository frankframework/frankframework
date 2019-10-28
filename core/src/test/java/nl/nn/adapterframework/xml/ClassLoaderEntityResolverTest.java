package nl.nn.adapterframework.xml;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarFile;

import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.classloaders.JarFileClassLoader;
import nl.nn.adapterframework.util.Misc;

public class ClassLoaderEntityResolverTest {

	private String publicId="fakePublicId";
	
	@Test
	public void localClassPathFileOnRootOfClasspath() throws SAXException, IOException {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();
		ClassLoaderEntityResolver resolver = new ClassLoaderEntityResolver(localClassLoader);

		String systemId="AppConstants.properties"; // this file is known to be in the root of the classpath
		InputSource inputSource = resolver.resolveEntity(publicId, systemId);

		assertNotNull(inputSource);
	}

	@Test
	public void localClassPathFileOnRootOfClasspathAbsolute() throws SAXException, IOException {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();
		ClassLoaderEntityResolver resolver = new ClassLoaderEntityResolver(localClassLoader);

		String systemId="/AppConstants.properties"; // this file is known to be in the root of the classpath
		InputSource inputSource = resolver.resolveEntity(publicId, systemId);

		assertNotNull(inputSource);
	}

	@Test
	public void localClassPathAbsolute() throws SAXException, IOException {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();
		ClassLoaderEntityResolver resolver = new ClassLoaderEntityResolver(localClassLoader);

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

		ClassLoaderEntityResolver resolver = new ClassLoaderEntityResolver(cl);

		String systemId="/Xslt/names.xsl";
		InputSource inputSource = resolver.resolveEntity(publicId, systemId);
		assertNotNull(inputSource);

	}

	@Test
	public void bytesClassPathAbsolute() throws SAXException, IOException, ConfigurationException {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL file = this.getClass().getResource("/classLoader-test.zip");
		assertNotNull("jar url not found", file);
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull("jar file not found",jarFile);

		JarFileClassLoader cl = new JarFileClassLoader(localClassLoader);
		cl.setJar(file.getFile());
		cl.configure(null, "");

		ClassLoaderEntityResolver resolver = new ClassLoaderEntityResolver(cl);

		String systemId="Xslt/names.xsl";
		InputSource inputSource = resolver.resolveEntity(publicId, systemId);
		assertNotNull(inputSource);
	}

	/*
	 * In a vague way we have to test absolute paths not relative to the working directory?
	 * This has to do with ClassUtils.getResourceURL() sometimes returning an absolute URL to a resource which does not exist..
	 */
	@Test
	public void localClassPathFullPath() throws SAXException, IOException, ConfigurationException {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		//Get the working directory
		final URL context = this.getClass().getResource("/");
		assertNotNull(context);

		//Get a random filename
		final String randomName = "myFile-"+Misc.createRandomUUID();

		//Find a file which does not exist, but also does not return NULL
		URL file = new URL(context, randomName);
		assertNotNull(context);

		ClassLoader dummyClassLoader = new ClassLoader(localClassLoader) {
			@Override
			public URL getResource(String name) {
				try {
					//Only return a valid file when the ClassLoader tries to find relative files
					if(randomName.equals(name)) {
						return new URL(context, "file.xml");
					} else {
						return new URL(context, "file-not-found");
					}
				} catch (MalformedURLException e) {
					fail("what? "+e.getMessage());
					return null; //keep the compiler happy
				}
			}
		};

		ClassLoaderEntityResolver resolver = new ClassLoaderEntityResolver(dummyClassLoader);

		try{
			file.openStream();
			fail("This should fail!"); //Make sure the file cannot be found!
		}
		catch (IOException e) {}

		String systemId = file.getFile(); //Get the full absolute path to the non-existing file
		InputSource inputSource = resolver.resolveEntity(publicId, systemId);
		assertNotNull(inputSource);
	}
}
