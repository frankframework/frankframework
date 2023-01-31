package nl.nn.adapterframework.validation;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

import org.apache.commons.io.IOUtils;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.classloaders.ClassLoaderBase;
import nl.nn.adapterframework.configuration.classloaders.JarFileClassLoader;
import nl.nn.adapterframework.core.IScopeProvider;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.testutil.TestScopeProvider;
import nl.nn.adapterframework.util.StreamUtil;

public class IntraGrammarPoolEntityResolverTest {

	private static final IScopeProvider scopeProvider = new TestScopeProvider();
	private List<Schema> schemas = new ArrayList<>();

	private String publicId="fakePublicId";
	public static final String JAR_FILE = "/ClassLoader/zip/classLoader-test.zip";
	private final String RESOURCE_THAT_ONLY_EXISTS_IN_JAR_FILE = "fileOnlyOnZipClassPath.xml";

	private XMLResourceIdentifier getXMLResourceIdentifier(String href, String namespace) {
		XMLResourceIdentifier resourceIdentifier = new ResourceIdentifier();
		resourceIdentifier.setPublicId(publicId);

		if(namespace != null) {
			resourceIdentifier.setNamespace(namespace);
		}

		resourceIdentifier.setBaseSystemId(ClassLoaderBase.CLASSPATH_RESOURCE_SCHEME+"Xslt/importDocument/importLookupRelative1.xsl");
		resourceIdentifier.setExpandedSystemId(ClassLoaderBase.CLASSPATH_RESOURCE_SCHEME+href);
		if(href.startsWith("../")) {
			resourceIdentifier.setExpandedSystemId(ClassLoaderBase.CLASSPATH_RESOURCE_SCHEME+"Xslt/"+href.substring(3));
		}
		resourceIdentifier.setLiteralSystemId(href); // this file is known to be in the root of the classpath
		return resourceIdentifier;
	}

	@ParameterizedTest
	@CsvSource({"./", "non/existing/folder/", "/non/existing/folder/", "./non/../folder/"})
	public void noClassPathSchemaResource(String base) throws Exception {
		schemas.add(new ResourceSchema("namespace1", "AppConstants.properties"));
		schemas.add(new ResourceSchema("namespace2", "dummy.properties"));

		IntraGrammarPoolEntityResolver resolver = new IntraGrammarPoolEntityResolver(scopeProvider, schemas);

		XMLResourceIdentifier resourceIdentifier = getXMLResourceIdentifier(base+"AppConstants.properties", "namespace1");

		XMLInputSource inputSource = resolver.resolveEntity(resourceIdentifier);
		assertNotNull(inputSource);

		String expected = TestFileUtils.getTestFile("/AppConstants.properties");
		TestAssertions.assertEqualsIgnoreCRLF(expected, xmlInputSource2String(inputSource));
	}

	@Test
	public void importFromSchemaWithoutNamespace() throws Exception {
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(new ClassLoader(originalClassLoader) {
			@Override
			public URL getResource(String name) {
				System.out.println(name);
				return super.getResource(name);
			}
		});

		XercesXmlValidator validator = new XercesXmlValidator();
		validator.setSchemasProvider(new SchemasProviderImpl(schemas));

		try {
			Thread.currentThread().setContextClassLoader(createBytesClassLoader());

//			schemas.add(new ResourceSchema(scopeProvider, "/Validation/IncludeWithoutNamespace/main.xsd"));
			schemas.add(new ResourceSchema(scopeProvider, RESOURCE_THAT_ONLY_EXISTS_IN_JAR_FILE));
			validator.start();
		} finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}

	@Test
	public void localClassPathRelativeLiteralSystemId() throws Exception {
		IntraGrammarPoolEntityResolver resolver = new IntraGrammarPoolEntityResolver(scopeProvider, schemas);

		XMLResourceIdentifier resourceIdentifier = getXMLResourceIdentifier("../../ClassLoader/subfolder/fileOnlyOnLocalClassPath.xml", "unused");

		XMLInputSource inputSource = resolver.resolveEntity(resourceIdentifier);
		assertNotNull(inputSource);

		String expected = TestFileUtils.getTestFile("/ClassLoader/subfolder/fileOnlyOnLocalClassPath.xml");
		TestAssertions.assertEqualsIgnoreCRLF(expected, xmlInputSource2String(inputSource));
	}

	@Test
	public void localClassPathAbsoluteRef() throws Exception {
		IntraGrammarPoolEntityResolver resolver = new IntraGrammarPoolEntityResolver(scopeProvider, schemas);

		XMLResourceIdentifier resourceIdentifier = getXMLResourceIdentifier("/Xslt/importDocument/lookup.xml", null);

		XMLInputSource inputSource = resolver.resolveEntity(resourceIdentifier);
		assertNotNull(inputSource);
	}

//	@Test
//	public void bytesClassPath() throws Exception {
//		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();
//
//		URL file = this.getClass().getResource(JAR_FILE);
//		assertNotNull(file, "jar url not found");
//		JarFile jarFile = new JarFile(file.getFile());
//		assertNotNull(jarFile, "jar file not found");
//
//		JarFileClassLoader cl = new JarFileClassLoader(localClassLoader);
//		cl.setJar(file.getFile());
//		cl.configure(null, "");
//
//		IntraGrammarPoolEntityResolver resolver = new IntraGrammarPoolEntityResolver(TestScopeProvider.wrap(cl), schemas);
//
//		XMLResourceIdentifier resourceIdentifier = getXMLResourceIdentifier("ClassLoader/Xslt/names.xsl");
//
//		XMLInputSource inputSource = resolver.resolveEntity(resourceIdentifier);
//		assertNotNull(inputSource);
//	}
//
//	@Test
//	public void bytesClassPathAbsolute() throws Exception  {
//		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();
//
//		URL file = this.getClass().getResource(JAR_FILE);
//		assertNotNull(file, "jar url not found");
//		JarFile jarFile = new JarFile(file.getFile());
//		assertNotNull(jarFile, "jar file not found");
//
//		JarFileClassLoader cl = new JarFileClassLoader(localClassLoader);
//		cl.setJar(file.getFile());
//		cl.configure(null, "");
//
//		IntraGrammarPoolEntityResolver resolver = new IntraGrammarPoolEntityResolver(TestScopeProvider.wrap(cl), schemas);
//
//		XMLResourceIdentifier resourceIdentifier = getXMLResourceIdentifier("/ClassLoader/Xslt/names.xsl");
//
//		XMLInputSource inputSource = resolver.resolveEntity(resourceIdentifier);
//		assertNotNull(inputSource);
//	}

	@Test
	public void classLoaderXmlEntityResolverCannotLoadExternalEntities() throws Exception {
		IntraGrammarPoolEntityResolver resolver = new IntraGrammarPoolEntityResolver(scopeProvider, schemas);

		XMLResourceIdentifier resourceIdentifier = getXMLResourceIdentifier("ftp://share.host.org/UDTSchema.xsd", null);
		URL url = this.getClass().getResource("/ClassLoader/request-ftp.xsd");
		assertNotNull(url);
		resourceIdentifier.setBaseSystemId(url.toExternalForm());

		XNIException thrown = assertThrows(XNIException.class, () -> {
			resolver.resolveEntity(resourceIdentifier);
		});

		assertThat(thrown.getMessage(), startsWith("Cannot find resource ["));
	}

	public static ClassLoader createBytesClassLoader() throws Exception {
		URL file = TestFileUtils.class.getResource(JAR_FILE);
		assertNotNull(file, "jar url not found");
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull(jarFile, "jar file not found");

		JarFileClassLoader cl = new JarFileClassLoader(new ClassLoader(null) {
			@Override
			public URL getResource(String name) {
				System.err.println(name);
				return super.getResource(name);
			}
		}); //No parent classloader
		cl.setJar(file.getFile());
		cl.configure(null, "");
		return cl;
	}

	private class ResourceIdentifier implements XMLResourceIdentifier {
		private @Getter @Setter String publicId;
		private @Getter @Setter String expandedSystemId;
		private @Getter @Setter String literalSystemId;
		private @Getter @Setter String baseSystemId;
		private @Getter @Setter String namespace;
	}

	private static String xmlInputSource2String(XMLInputSource inputSource) throws IOException {
		Reader reader = null;
		if(inputSource.getCharacterStream() != null) {
			reader = inputSource.getCharacterStream();
		} else if(inputSource.getByteStream() != null) {
			reader = StreamUtil.getCharsetDetectingInputStreamReader(inputSource.getByteStream());
		}
		if(reader == null) {
			throw new IOException("unable to read XMLInputSource, no Character nor Byte stream available");
		}
		return IOUtils.toString(reader);
	}

	private static class SchemasProviderImpl implements SchemasProvider {
		private final @Getter List<Schema> schemas;

		public SchemasProviderImpl(List<Schema> schemas) {
			this.schemas = schemas;
		}

		@Override
		public String getSchemasId() throws ConfigurationException {
			return "dummySchemaID";
		}

		@Override
		public String getSchemasId(PipeLineSession session) throws PipeRunException {
			return null;
		}

		@Override
		public List<Schema> getSchemas(PipeLineSession session) throws PipeRunException {
			return null;
		}
	}

	private static class ResourceSchema implements Schema {
		private final String alias;
		private final Resource resource;

		public ResourceSchema(String alias, String lookup) {
//			this(scopeProvider, resource);
			this.resource = Resource.getResource(scopeProvider, lookup);
			this.alias = alias;
		}
		public ResourceSchema(IScopeProvider scopeProvider, String lookup) {
			this.resource = Resource.getResource(scopeProvider, lookup);
			this.alias = resource.getSystemId();
		}

		@Override
		public Reader getReader() throws IOException {
			return StreamUtil.getCharsetDetectingInputStreamReader(resource.openStream());
		}

		@Override
		public String getSystemId() {
			return alias;
		}

	}
}