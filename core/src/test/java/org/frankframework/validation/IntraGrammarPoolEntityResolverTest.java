package org.frankframework.validation;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
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

import org.frankframework.configuration.classloaders.IConfigurationClassLoader;
import org.frankframework.configuration.classloaders.JarFileClassLoader;
import org.frankframework.core.IScopeProvider;
import org.frankframework.core.Resource;
import org.frankframework.testutil.TestAssertions;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.TestScopeProvider;
import org.frankframework.util.StreamUtil;

public class IntraGrammarPoolEntityResolverTest {

	private static final IScopeProvider scopeProvider = new TestScopeProvider();
	private static final List<Schema> EMPTY_SCHEMAS_LIST = Collections.emptyList();

	private final String publicId = "fakePublicId";
	public static final String JAR_FILE = "/ClassLoader/zip/classLoader-test.zip";

	private XMLResourceIdentifier getXMLResourceIdentifier(String href, String namespace) {
		XMLResourceIdentifier resourceIdentifier = new ResourceIdentifier();
		resourceIdentifier.setPublicId(publicId);

		if(namespace != null) {
			resourceIdentifier.setNamespace(namespace);
		}

		resourceIdentifier.setBaseSystemId(IConfigurationClassLoader.CLASSPATH_RESOURCE_SCHEME+"Xslt/importDocument/importLookupRelative1.xsl");
		resourceIdentifier.setExpandedSystemId(IConfigurationClassLoader.CLASSPATH_RESOURCE_SCHEME+href);
		if(href.startsWith("../")) {
			resourceIdentifier.setExpandedSystemId(IConfigurationClassLoader.CLASSPATH_RESOURCE_SCHEME+"Xslt/"+href.substring(3));
		}
		resourceIdentifier.setLiteralSystemId(IConfigurationClassLoader.CLASSPATH_RESOURCE_SCHEME+href); // this file is known to be in the root of the classpath
		return resourceIdentifier;
	}

	@ParameterizedTest
	@CsvSource({"./", "non/existing/folder/", "/non/existing/folder/", "./non/../folder/", IConfigurationClassLoader.CLASSPATH_RESOURCE_SCHEME+"./non/../folder/"})
	public void noClassPathSchemaResource(String base) throws Exception {
		List<Schema> schemas = new ArrayList<>();
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
	public void localClassPathRelativeLiteralSystemId() throws Exception {
		IntraGrammarPoolEntityResolver resolver = new IntraGrammarPoolEntityResolver(scopeProvider, EMPTY_SCHEMAS_LIST);

		XMLResourceIdentifier resourceIdentifier = getXMLResourceIdentifier("../../ClassLoader/subfolder/fileOnlyOnLocalClassPath.xml", null);

		XMLInputSource inputSource = resolver.resolveEntity(resourceIdentifier);
		assertNotNull(inputSource);

		String expected = TestFileUtils.getTestFile("/ClassLoader/subfolder/fileOnlyOnLocalClassPath.xml");
		TestAssertions.assertEqualsIgnoreCRLF(expected, xmlInputSource2String(inputSource));
	}

	@Test
	public void importFromSchemaWithoutNamespaceWithClassPathPrefixInJarBytesClassLoaderWithParent() throws Exception {
		List<Schema> schemas = new ArrayList<>();
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			ClassLoader bytesClassLoader = createBytesClassLoaderWithNoLocalAccess();
			Thread.currentThread().setContextClassLoader(bytesClassLoader);
			IntraGrammarPoolEntityResolver resolver = new IntraGrammarPoolEntityResolver(scopeProvider, schemas);

			XMLResourceIdentifier resourceIdentifier = getXMLResourceIdentifier("../../ClassLoader/subfolder/fileOnlyOnLocalClassPath.xml", "unused");

			XMLInputSource inputSource = resolver.resolveEntity(resourceIdentifier);
			assertNotNull(inputSource);

			String expected = TestFileUtils.getTestFile("/ClassLoader/subfolder/fileOnlyOnLocalClassPath.xml");
			TestAssertions.assertEqualsIgnoreCRLF(expected, xmlInputSource2String(inputSource));
		} finally {
			Thread.currentThread().setContextClassLoader(localClassLoader);
		}
	}

	@Test
	public void importFromSchemaWithoutNamespaceWithClassPathPrefixInJarBytesClassLoaderWithNoParent() throws Exception {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			ClassLoader bytesClassLoader = createBytesClassLoaderWithNoLocalAccess();
			Thread.currentThread().setContextClassLoader(bytesClassLoader);
			IntraGrammarPoolEntityResolver resolver = new IntraGrammarPoolEntityResolver(TestScopeProvider.wrap(bytesClassLoader), EMPTY_SCHEMAS_LIST);

			XMLResourceIdentifier resourceIdentifier = getXMLResourceIdentifier("../../ClassLoader/subfolder/fileOnlyOnLocalClassPath.xml", "unused");

			XNIException thrown = assertThrows(XNIException.class, () -> resolver.resolveEntity(resourceIdentifier));
			assertThat(thrown.getMessage(), startsWith("Cannot find resource ["));
		} finally {
			Thread.currentThread().setContextClassLoader(localClassLoader);
		}
	}

	@Test //See issue #3973. Should throw an XNIException to trigger XercesValidationErrorHandler#error which rethrows the Exception.
	public void localClassPathAbsoluteRef() throws Exception {
		IntraGrammarPoolEntityResolver resolver = new IntraGrammarPoolEntityResolver(scopeProvider, EMPTY_SCHEMAS_LIST);

		XMLResourceIdentifier resourceIdentifier = getXMLResourceIdentifier("/this/schema/does/not/exist.xsd", null);

		XNIException thrown = assertThrows(XNIException.class, () -> resolver.resolveEntity(resourceIdentifier));
		assertThat(thrown.getMessage(), startsWith("Cannot find resource ["));
	}

	@Test
	public void classLoaderXmlEntityResolverCannotLoadExternalEntities() throws Exception {
		IntraGrammarPoolEntityResolver resolver = new IntraGrammarPoolEntityResolver(scopeProvider, EMPTY_SCHEMAS_LIST);

		XMLResourceIdentifier resourceIdentifier = getXMLResourceIdentifier("ftp://share.host.org/UDTSchema.xsd", null);
		URL url = this.getClass().getResource("/ClassLoader/request-ftp.xsd");
		assertNotNull(url);
		resourceIdentifier.setBaseSystemId(url.toExternalForm());

		XNIException thrown = assertThrows(XNIException.class, () -> resolver.resolveEntity(resourceIdentifier));

		assertThat(thrown.getMessage(), startsWith("Cannot find resource ["));
	}

	public static ClassLoader createBytesClassLoaderWithNoLocalAccess() throws Exception {
		URL file = TestFileUtils.class.getResource(JAR_FILE);
		assertNotNull(file, "jar url not found");
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull(jarFile, "jar file not found");

		JarFileClassLoader cl = new JarFileClassLoader(new ClassLoader(null) {}); //No parent classloader, getResource and getResources will not fall back
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

	private static class ResourceSchema implements Schema {
		private final String alias;
		private final Resource resource;

		public ResourceSchema(String alias, String lookup) {
			this.resource = Resource.getResource(scopeProvider, lookup);
			this.alias = alias;
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
