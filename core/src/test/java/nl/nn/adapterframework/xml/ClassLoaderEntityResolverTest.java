package nl.nn.adapterframework.xml;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarFile;

import nl.nn.adapterframework.util.UUIDUtil;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.classloaders.JarFileClassLoader;
import nl.nn.adapterframework.core.IScopeProvider;
import nl.nn.adapterframework.testutil.TestScopeProvider;

public class ClassLoaderEntityResolverTest {

	private String publicId="fakePublicId";
	protected final String JAR_FILE = "/ClassLoader/zip/classLoader-test.zip";
	private IScopeProvider localScopeProvider = new TestScopeProvider();

	@Test
	public void localClassPathFileOnRootOfClasspath() throws SAXException, IOException {
		ClassLoaderEntityResolver resolver = new ClassLoaderEntityResolver(localScopeProvider);

		String systemId="AppConstants.properties"; // this file is known to be in the root of the classpath
		InputSource inputSource = resolver.resolveEntity(publicId, systemId);

		assertNotNull(inputSource);
	}

	@Test
	public void localClassPathFileOnRootOfClasspathAbsolute() throws Exception {
		ClassLoaderEntityResolver resolver = new ClassLoaderEntityResolver(localScopeProvider);

		String systemId="/AppConstants.properties"; // this file is known to be in the root of the classpath
		InputSource inputSource = resolver.resolveEntity(publicId, systemId);

		assertNotNull(inputSource);
	}

	@Test
	public void localClassPathAbsolute() throws SAXException, IOException {
		ClassLoaderEntityResolver resolver = new ClassLoaderEntityResolver(localScopeProvider);

		String systemId="/Xslt/importDocument/lookup.xml";

		InputSource inputSource = resolver.resolveEntity(publicId, systemId);
		assertNotNull(inputSource);
	}


	@Test
	public void bytesClassPath() throws Exception {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL file = this.getClass().getResource(JAR_FILE);
		assertNotNull("jar url not found", file);
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull("jar file not found",jarFile);

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
		assertNotNull("jar url not found", file);
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull("jar file not found",jarFile);

		JarFileClassLoader cl = new JarFileClassLoader(localClassLoader);
		cl.setJar(file.getFile());
		cl.configure(null, "");

		ClassLoaderEntityResolver resolver = new ClassLoaderEntityResolver(TestScopeProvider.wrap(cl));

		String systemId="ClassLoader/Xslt/names.xsl";
		InputSource inputSource = resolver.resolveEntity(publicId, systemId);
		assertNotNull(inputSource);
	}

	/*
	 * In a vague way we have to test absolute paths not relative to the working directory?
	 * This has to do with ClassUtils.getResourceURL() sometimes returning an absolute URL to a resource which does not exist..
	 */
	@Test
	@Ignore("Fixed the original problem in XmlUtils.identityTransform(), that did not set a systemId for relative resolutions")
	public void localClassPathFullPath() throws Exception {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		//Get the working directory
		final URL context = this.getClass().getResource("/");
		assertNotNull(context);

		//Get a random filename
		final String randomName = "myFile-"+ UUIDUtil.createRandomUUID();

		//Find a file which does not exist, but also does not return NULL
		URL file = new URL(context, randomName);
		assertNotNull(file);


		ClassLoader dummyClassLoader = new ClassLoader(localClassLoader) {
			@Override
			public URL getResource(String name) {
				System.out.println("dummyClassLoader name ["+name+"]");
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

		ClassLoaderEntityResolver resolver = new ClassLoaderEntityResolver(TestScopeProvider.wrap(dummyClassLoader));

		try{
			file.openStream();
			fail("This should fail!"); //Make sure the file cannot be found!
		}
		catch (IOException e) {}

		String systemId = file.getFile(); //Get the full absolute path to the non-existing file

		System.out.println("file to resolve ["+systemId+"]");

		InputSource inputSource = resolver.resolveEntity(publicId, systemId);
		assertNotNull(inputSource);
	}
}
