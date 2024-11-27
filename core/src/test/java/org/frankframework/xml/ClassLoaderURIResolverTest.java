package org.frankframework.xml;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.classloaders.JarFileClassLoader;
import org.frankframework.core.IScopeProvider;
import org.frankframework.core.Resource;
import org.frankframework.testutil.TestScopeProvider;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.XmlUtils;

public class ClassLoaderURIResolverTest {

	private final String JAR_FILE = "/ClassLoader/zip/classLoader-test.zip";
	private static final IScopeProvider scopeProvider = new TestScopeProvider();

	private final Logger log = LogUtil.getLogger(this);
	private enum BaseType { LOCAL, BYTES, CLASSPATH, FILE_SCHEME, NULL }
	private enum RefType  {
		ROOT, ABS_PATH, DOTDOT, SAME_FOLDER, OVERRIDABLE, CLASSPATH, FILE_SCHEME(TransformerException.class);
		private Class<? extends Exception> exception;

		RefType() {
			this(null);
		}
		RefType(Class<? extends Exception> exception) {
			this.exception = exception;
		}
		Class<? extends Exception> expectsException() {
			return exception;
		}
	}

	private static Stream<Arguments> testParameters() {
		List<Arguments> result = new ArrayList<>();
		for(BaseType baseType:BaseType.values()) {
			for (RefType refType: RefType.values()) {
				result.add(Arguments.of(baseType, refType));
			}
		}
		return result.stream();
	}

	private void testUri(String baseType, String refType, IScopeProvider cl, String base, String ref, String expected) throws TransformerException {
		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(cl);

		Source source = resolver.resolve(ref, base);
		assertNotNull(source);
		if (expected!=null) {
			assertEquals(expected, XmlUtils.source2String(source), "BaseType ["+baseType+"] refType ["+refType+"]");
		} else {
			assertNotNull(XmlUtils.source2String(source), "BaseType ["+baseType+"] refType ["+refType+"]");
		}
	}

	private IScopeProvider getClassLoaderProvider(BaseType baseType) throws Exception {
		if (baseType==BaseType.BYTES) {
			return getBytesClassLoader();
		}
		return scopeProvider;
	}

	private String getBase(IScopeProvider classLoaderProvider, BaseType baseType) throws ConfigurationException {
		URL result=null;
		switch (baseType) {
		case LOCAL:
			return "/ClassLoader/Xslt/root.xsl";
		case BYTES:
			result = ClassLoaderUtils.getResourceURL(classLoaderProvider, "/ClassLoader/Xslt/root.xsl");
			return result.toExternalForm();
		case CLASSPATH:
			return "classpath:ClassLoader/Xslt/root.xsl";
		case FILE_SCHEME:
			result = ClassLoaderUtils.getResourceURL(classLoaderProvider, "/ClassLoader/Xslt/root.xsl");
			return result.toExternalForm();
		case NULL:
			return null;
		default:
			throw new ConfigurationException("getBase() appears to be missing case for baseType ["+baseType+"]");
		}
	}

	private String getRef(BaseType baseType, RefType refType) throws ConfigurationException {
		switch (refType) {
		case ROOT:
			return "/ClassLoaderTestFile.xml";
		case ABS_PATH:
			return "/ClassLoader/ClassLoaderTestFile.xml";
		case DOTDOT:
			if (baseType==BaseType.NULL) {
				return null;
			}
			return "../subfolder/ClassLoaderTestFile.xml";
		case SAME_FOLDER:
			if (baseType==BaseType.NULL) {
				return null;
			}
			return "names.xsl";
		case OVERRIDABLE:
			return "/ClassLoader/overridablefile.xml";
		case CLASSPATH:
			return "classpath:/ClassLoader/overridablefile.xml";
		case FILE_SCHEME:
			return ClassLoaderUtils.getResourceURL("/ClassLoader/overridablefile.xml").toExternalForm();
		default:
			throw new ConfigurationException("getRef() appears to be missing case for refType ["+refType+"]");
		}
	}

	private String getExpected(BaseType baseType, RefType refType) throws ConfigurationException {
		switch(refType) {
		case ROOT:
			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>/ClassLoaderTestFile.xml</file>";
		case ABS_PATH:
			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>/ClassLoader/ClassLoaderTestFile.xml</file>";
		case DOTDOT:
			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>/ClassLoader/subfolder/ClassLoaderTestFile.xml</file>";
		case SAME_FOLDER:
			return null;
		case OVERRIDABLE:
		case CLASSPATH:
			if (baseType==BaseType.BYTES) {
				return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>zip:/overrideablefile.xml</file>";
			}
			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>local:/overrideablefile.xml</file>";
		case FILE_SCHEME:
			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>local:/overrideablefile.xml</file>";
		default:
			throw new ConfigurationException("getExpected() appears to be missing case for refType ["+refType+"]");
		}
	}

	@ParameterizedTest
	@MethodSource("testParameters")
	public void resolveToSource(BaseType baseType, RefType refType) throws Exception {
		IScopeProvider classLoaderProvider = getClassLoaderProvider(baseType);
		String baseUrl = getBase(classLoaderProvider, baseType);
		log.debug("BaseType [{}] classLoader [{}] BaseUrl [{}]", baseType, classLoaderProvider, baseUrl);

		String ref = getRef(baseType,refType);
		String expected = getExpected(baseType,refType);
		log.debug("BaseType [{}] refType [{}] ref [{}] expected [{}]", baseType, refType, ref, expected);
		if (ref!=null) {
			if(refType.expectsException() != null) {
				assertThrows(refType.expectsException(), () -> testUri(baseType.name(), refType.name(), classLoaderProvider, baseUrl, ref, expected));
			} else {
				testUri(baseType.name(), refType.name(), classLoaderProvider, baseUrl, ref, expected);
			}
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"AppConstants.properties", "/AppConstants.properties", "/Xslt/importDocument/lookup.xml"})
	public void localClassPathLookup(String filename) throws Exception {
		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(scopeProvider);

		Resource resource = resolver.resolveToResource(filename, null);
		assertNotNull(resource);
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

		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(TestScopeProvider.wrap(cl));

		Resource resource = resolver.resolveToResource("ClassLoader/Xslt/names.xsl", null);
		assertNotNull(resource);
	}

	@Test
	public void bytesClassPathAbsolute() throws Exception  {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL file = this.getClass().getResource(JAR_FILE);
		assertNotNull(file, "jar url not found");
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull(jarFile, "jar file not found");

		JarFileClassLoader cl = new JarFileClassLoader(localClassLoader);
		cl.setJar(file.getFile());
		cl.configure(null, "");

		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(TestScopeProvider.wrap(cl));

		Resource resource = resolver.resolveToResource("ClassLoader/Xslt/names.xsl", null);
		assertNotNull(resource);
	}

	@Test
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

	private IScopeProvider getBytesClassLoader() throws Exception {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL file = this.getClass().getResource(JAR_FILE);
		assertNotNull(file, "jar url ["+JAR_FILE+"] not found");
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull(jarFile, "jar file not found");

		JarFileClassLoader cl = new JarFileClassLoader(localClassLoader);
		cl.setJar(file.getFile());
		cl.setBasePath(".");
		cl.configure(null, "");
		return TestScopeProvider.wrap(cl);
	}
}
