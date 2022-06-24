package nl.nn.adapterframework.xml;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.net.URL;
import java.util.jar.JarFile;

import javax.xml.transform.TransformerException;

import org.apache.xerces.xni.XNIException;
import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.configuration.classloaders.JarFileClassLoader;
import nl.nn.adapterframework.core.IScopeProvider;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.testutil.TestScopeProvider;

public class ClassLoaderURIResolverTest2 {

	private static final IScopeProvider scopeProvider = new TestScopeProvider();
	//private String base="/ClassLoader/zip/Xslt/names.xslt";
	protected final String JAR_FILE = "/ClassLoader/zip/classLoader-test.zip";



	@Test
	public void localClassPathFileOnRootOfClasspath() throws Exception {
		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(scopeProvider);

		Resource resource = resolver.resolveToResource("AppConstants.properties", null);
		assertNotNull(resource);
	}

	@Test
	public void localClassPathFileOnRootOfClasspathAbsolute() throws Exception {
		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(scopeProvider);

		Resource resource = resolver.resolveToResource("/AppConstants.properties", null);
		assertNotNull(resource);
	}

	@Test
	public void localClassPathAbsolute() throws Exception {
		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(scopeProvider);

		Resource resource = resolver.resolveToResource("/Xslt/importDocument/lookup.xml", null);
		assertNotNull(resource);
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

		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(TestScopeProvider.wrap(cl));

		Resource resource = resolver.resolveToResource("ClassLoader/Xslt/names.xsl", null);
		assertNotNull(resource);
	}

	@Test
	public void bytesClassPathAbsolute() throws Exception  {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL file = this.getClass().getResource(JAR_FILE);
		assertNotNull("jar url not found", file);
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull("jar file not found",jarFile);

		JarFileClassLoader cl = new JarFileClassLoader(localClassLoader);
		cl.setJar(file.getFile());
		cl.configure(null, "");

		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(TestScopeProvider.wrap(cl));

		Resource resource = resolver.resolveToResource("ClassLoader/Xslt/names.xsl", null);
		assertNotNull(resource);
	}

	@Ignore
	@Test(expected = XNIException.class)
	public void classLoaderURIResolverCanLoadLocalEntities() throws Exception {
		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(scopeProvider);

		URL url = this.getClass().getResource("/ClassLoader/request.xsd");
		assertNotNull(url);

		Resource resource = resolver.resolveToResource("UDTSchema.xsd", url.toExternalForm());
		assertNotNull(resource);
	}

	@Test
	public void classLoaderURIResolverCannotLoadExternalEntities() throws Exception {
		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(scopeProvider);

		URL url = this.getClass().getResource("/ClassLoader/request-ftp.xsd");
		assertNotNull(url);

		TransformerException thrown = assertThrows(TransformerException.class, () -> {
			resolver.resolveToResource("ftp://share.host.org/UDTSchema.xsd", url.toExternalForm());
		});

		assertThat(thrown.getMessage(), startsWith("Cannot lookup resource [ftp://share.host.org/UDTSchema.xsd] not allowed with protocol [ftp]"));
	}


}
