package nl.nn.adapterframework.xml;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.jar.JarFile;

import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.classloaders.JarFileClassLoader;
import nl.nn.adapterframework.core.IScopeProvider;
import nl.nn.adapterframework.testutil.TestScopeProvider;

public class ClassLoaderXmlEntityResolverTest {

	private static final IScopeProvider scopeProvider = new TestScopeProvider();
	private String publicId="fakePublicId";
	//private String base="/ClassLoader/zip/Xslt/names.xslt";
	protected final String JAR_FILE = "/ClassLoader/zip/classLoader-test.zip";
	
	private XMLResourceIdentifier getXMLResourceIdentifier(String href) {
		XMLResourceIdentifier resourceIdentifier = new ResourceIdentifier(); 
		resourceIdentifier.setPublicId(publicId);
		
//		resourceIdentifier.setExpandedSystemId(href); // this file is known to be in the root of the classpath
		resourceIdentifier.setLiteralSystemId(href); // this file is known to be in the root of the classpath
		return resourceIdentifier;
	}
	
	
	@Test
	public void localClassPathFileOnRootOfClasspath() throws SAXException, IOException {
		ClassLoaderXmlEntityResolver resolver = new ClassLoaderXmlEntityResolver(scopeProvider);
		
		XMLResourceIdentifier resourceIdentifier = getXMLResourceIdentifier("AppConstants.properties");
		
		XMLInputSource inputSource = resolver.resolveEntity(resourceIdentifier);
		assertNotNull(inputSource);
	}

	@Test
	public void localClassPathFileOnRootOfClasspathAbsolute() throws SAXException, IOException {
		ClassLoaderXmlEntityResolver resolver = new ClassLoaderXmlEntityResolver(scopeProvider);

		XMLResourceIdentifier resourceIdentifier = getXMLResourceIdentifier("/AppConstants.properties");

		XMLInputSource inputSource = resolver.resolveEntity(resourceIdentifier);
		assertNotNull(inputSource);
	}

	@Test
	public void localClassPathAbsolute() throws SAXException, IOException {
		ClassLoaderXmlEntityResolver resolver = new ClassLoaderXmlEntityResolver(scopeProvider);

		XMLResourceIdentifier resourceIdentifier = getXMLResourceIdentifier("/Xslt/importDocument/lookup.xml");
		
		XMLInputSource inputSource = resolver.resolveEntity(resourceIdentifier);
		assertNotNull(inputSource);
	}

	
	@Test
	public void bytesClassPath() throws SAXException, IOException, ConfigurationException {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL file = this.getClass().getResource(JAR_FILE);
		assertNotNull("jar url not found", file);
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull("jar file not found",jarFile);

		JarFileClassLoader cl = new JarFileClassLoader(localClassLoader);
		cl.setJar(file.getFile());
		cl.configure(null, "");

		ClassLoaderXmlEntityResolver resolver = new ClassLoaderXmlEntityResolver(TestScopeProvider.wrap(cl));

		XMLResourceIdentifier resourceIdentifier = getXMLResourceIdentifier("ClassLoader/Xslt/names.xsl");

		XMLInputSource inputSource = resolver.resolveEntity(resourceIdentifier);
		assertNotNull(inputSource);
	}

	@Test
	public void bytesClassPathAbsolute() throws SAXException, IOException, ConfigurationException  {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL file = this.getClass().getResource(JAR_FILE);
		assertNotNull("jar url not found", file);
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull("jar file not found",jarFile);

		JarFileClassLoader cl = new JarFileClassLoader(localClassLoader);
		cl.setJar(file.getFile());
		cl.configure(null, "");

		ClassLoaderXmlEntityResolver resolver = new ClassLoaderXmlEntityResolver(TestScopeProvider.wrap(cl));

		XMLResourceIdentifier resourceIdentifier = getXMLResourceIdentifier("/ClassLoader/Xslt/names.xsl");

		XMLInputSource inputSource = resolver.resolveEntity(resourceIdentifier);
		assertNotNull(inputSource);
	}

	@Ignore
	@Test(expected = XNIException.class)
	public void classLoaderXmlEntityResolverCanLoadLocalEntities() throws Exception {
		ClassLoaderXmlEntityResolver resolver = new ClassLoaderXmlEntityResolver(scopeProvider);

		XMLResourceIdentifier resourceIdentifier = getXMLResourceIdentifier("UDTSchema.xsd");
		URL url = this.getClass().getResource("/ClassLoader/request.xsd");
		assertNotNull(url);
		resourceIdentifier.setBaseSystemId(url.toExternalForm());

		XMLInputSource inputSource = resolver.resolveEntity(resourceIdentifier);
		assertNotNull(inputSource);
	}

	@Test
	public void classLoaderXmlEntityResolverCannotLoadExternalEntities() throws Exception {
		ClassLoaderXmlEntityResolver resolver = new ClassLoaderXmlEntityResolver(scopeProvider);

		XMLResourceIdentifier resourceIdentifier = getXMLResourceIdentifier("ftp://share.host.org/UDTSchema.xsd");
		URL url = this.getClass().getResource("/ClassLoader/request-ftp.xsd");
		assertNotNull(url);
		resourceIdentifier.setBaseSystemId(url.toExternalForm());

		XNIException thrown = assertThrows(XNIException.class, () -> {
			resolver.resolveEntity(resourceIdentifier);
		});

		String errorMessage = "Cannot lookup resource [ftp://share.host.org/UDTSchema.xsd] with protocol [ftp], no allowedProtocols";
		assertTrue("SaxParseException should start with [Cannot get resource ...] but is ["+thrown.getMessage()+"]", thrown.getMessage().startsWith(errorMessage));
	}

	private class ResourceIdentifier implements XMLResourceIdentifier {

		private String publicId;
		private String expandedSystemId;
		private String literalSystemId;
		private String baseSystemId;
		private String namespace;
		
		@Override
		public void setPublicId(String publicId) {
			this.publicId=publicId;
		}
		@Override
		public String getPublicId() {
			return publicId;
		}

		@Override
		public void setExpandedSystemId(String systemId) {
			this.expandedSystemId=systemId;
		}
		@Override
		public String getExpandedSystemId() {
			return expandedSystemId;
		}

		@Override
		public void setLiteralSystemId(String systemId) {
			this.literalSystemId=systemId;
		}
		@Override
		public String getLiteralSystemId() {
			return literalSystemId;
		}

		@Override
		public void setBaseSystemId(String systemId) {
			this.baseSystemId=systemId;
		}
		@Override
		public String getBaseSystemId() {
			return baseSystemId;
		}

		@Override
		public void setNamespace(String namespace) {
			this.namespace=namespace;
		}
		@Override
		public String getNamespace() {
			return namespace;
		}
		
	}

}
