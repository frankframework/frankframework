package nl.nn.adapterframework.xml;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;
import java.util.jar.JarFile;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.classloaders.JarFileClassLoader;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.xml.ClassLoaderURIResolver;

public class ClassLoaderURIResolverTest {

	@Test
	public void localClassPathRelative() throws TransformerException {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL xslt = ClassUtils.getResourceURL(localClassLoader, "/Xslt/importDocument/importLookupAbsolute1.xsl");
		assertNotNull(xslt);

		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(localClassLoader);
		Source source = resolver.resolve("lookup.xml", xslt.toString());
		assertNotNull(source);
	}

	@Test
	public void localClassPathAbsolute() throws TransformerException {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL xslt = ClassUtils.getResourceURL(localClassLoader, "/Xslt/importDocument/importLookupAbsolute1.xsl");
		assertNotNull(xslt);

		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(localClassLoader);
		Source source = resolver.resolve("/Xslt/importDocument/lookup.xml", xslt.toString());
		assertNotNull(source);
	}

	@Test
	public void bytesClassPathRelative() throws TransformerException, IOException, ConfigurationException {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL file = this.getClass().getResource("/classLoader-test.zip");
		assertNotNull("jar url not found", file);
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull("jar file not found",jarFile);

		JarFileClassLoader cl = new JarFileClassLoader(localClassLoader);
		cl.setJar(file.getFile());
		cl.configure(null, "");

		URL xslt = ClassUtils.getResourceURL(cl, "/Xslt/root.xsl");
		assertNotNull("root.xsl not found", xslt);

		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(cl);

		Source source = resolver.resolve("names.xsl", xslt.toString());
		assertNotNull(source);
	}

	@Test
	public void bytesClassPathAbsolute() throws TransformerException, IOException, ConfigurationException {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL file = this.getClass().getResource("/classLoader-test.zip");
		assertNotNull("jar url not found", file);
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull("jar file not found",jarFile);

		JarFileClassLoader cl = new JarFileClassLoader(localClassLoader);
		cl.setJar(file.getFile());
		cl.configure(null, "");

		URL xslt = ClassUtils.getResourceURL(cl, "/Xslt/root.xsl");
		assertNotNull("root.xsl not found", xslt);

		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(cl);

		Source source = resolver.resolve("/Xslt/names.xsl", xslt.toString());
		assertNotNull(source);
	}

	@Test
	public void bytesClassPathBaseAndResourceFromLocalClasspathAbsolute() throws TransformerException, IOException, ConfigurationException {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL file = this.getClass().getResource("/classLoader-test.zip");
		assertNotNull("jar url not found", file);
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull("jar file not found",jarFile);

		JarFileClassLoader cl = new JarFileClassLoader(localClassLoader);
		cl.setJar(file.getFile());
		cl.configure(null, "");

		URL xslt = ClassUtils.getResourceURL(cl, "/Xslt/root.xsl");
		assertNotNull("root.xsl not found", xslt);

		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(cl);

		Source source = resolver.resolve("/Xslt/importDocument/lookup.xml", xslt.toString());
		assertNotNull(source);
	}

	@Test
	public void bytesClassPathBaseAndResourceFromLocalClasspathRelative() throws TransformerException, IOException, ConfigurationException {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL file = this.getClass().getResource("/classLoader-test.zip");
		assertNotNull("jar url not found", file);
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull("jar file not found",jarFile);

		JarFileClassLoader cl = new JarFileClassLoader(localClassLoader);
		cl.setJar(file.getFile());
		cl.configure(null, "");

		URL xslt = ClassUtils.getResourceURL(cl, "/Xslt/root.xsl");
		assertNotNull("root.xsl not found", xslt);

		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(cl);

		Source source = resolver.resolve("importDocument/lookup.xml", xslt.toString());
		assertNotNull(source);
	}

}
