package nl.nn.adapterframework.util;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;
import java.util.jar.JarFile;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.classloaders.BytesClassLoader;
import nl.nn.adapterframework.configuration.classloaders.JarFileClassLoader;

import org.junit.Test;

public class ClassLoaderURIResolverTest {

	@Test
	public void localClassPath() throws TransformerException {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();
		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(localClassLoader);

		URL xslt = ClassUtils.getResourceURL(localClassLoader, "/Xslt/duplicateImport/root.xsl");
		assertNotNull(xslt);

		Source source = resolver.resolve("names.xsl", xslt.toString());
		assertNotNull(source);
	}

	@Test
	public void bytesClassPath() throws TransformerException, IOException, ConfigurationException {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL file = this.getClass().getResource("/classLoader-test.zip");
		assertNotNull("jar url not found", file);
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull("jar file not found",jarFile);

		JarFileClassLoader cl = new JarFileClassLoader(localClassLoader);
		cl.setJar(file.getFile());
		cl.configure(null, "");

		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(cl);

		URL xslt = ClassUtils.getResourceURL(cl, "/Xslt/root.xsl");
		assertNotNull("root.xsl not found", xslt);

		Source source = resolver.resolve("names.xsl", xslt.toString());
		assertNotNull(source);
	}
}
