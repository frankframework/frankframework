package nl.nn.adapterframework.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.classloaders.JarFileClassLoader;
import nl.nn.adapterframework.core.IScopeProvider;
import nl.nn.adapterframework.testutil.TestScopeProvider;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

public class ClassLoaderURIResolverTest {

	private Logger log = LogUtil.getLogger(this);
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

	protected final String JAR_FILE = "/ClassLoader/zip/classLoader-test.zip";

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
			assertEquals("BaseType ["+baseType+"] refType ["+refType+"]", expected, XmlUtils.source2String(source, false));
		} else {
			assertNotNull("BaseType ["+baseType+"] refType ["+refType+"]", XmlUtils.source2String(source, false));
		}
	}

	private IScopeProvider getClassLoaderProvider(BaseType baseType) throws Exception {
		if (baseType==BaseType.BYTES) {
			return getBytesClassLoader();
		}
		return new TestScopeProvider();
	}

	private String getBase(IScopeProvider classLoaderProvider, BaseType baseType) throws ConfigurationException, IOException {
		URL result=null;
		switch (baseType) {
		case LOCAL:
			return "/ClassLoader/Xslt/root.xsl";
		case BYTES:
			result = ClassUtils.getResourceURL(classLoaderProvider, "/ClassLoader/Xslt/root.xsl");
			return result.toExternalForm();
		case CLASSPATH:
			return "classpath:ClassLoader/Xslt/root.xsl";
		case FILE_SCHEME:
			result = ClassUtils.getResourceURL(classLoaderProvider, "/ClassLoader/Xslt/root.xsl");
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
			return ClassUtils.getResourceURL("/ClassLoader/overridablefile.xml").toExternalForm();
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
	public void runTests(BaseType baseType, RefType refType) throws Exception {
		IScopeProvider classLoaderProvider = getClassLoaderProvider(baseType);
		String baseUrl = getBase(classLoaderProvider, baseType);
		log.debug("BaseType [{}] classLoader [{}] BaseUrl [{}]", baseType, classLoaderProvider, baseUrl);

		String ref = getRef(baseType,refType);
		String expected = getExpected(baseType,refType);
		log.debug("BaseType [{}] refType [{}] ref [{}] expected [{}]", baseType, refType, ref, expected);
		if (ref!=null) {
			if(refType.expectsException() != null) {
				assertThrows(refType.expectsException(), () -> {
					testUri(baseType.name(), refType.name(), classLoaderProvider, baseUrl, ref, expected);
				});
			} else {
				testUri(baseType.name(), refType.name(), classLoaderProvider, baseUrl, ref, expected);
			}
		}
	}


	private IScopeProvider getBytesClassLoader() throws Exception {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL file = this.getClass().getResource(JAR_FILE);
		assertNotNull("jar url ["+JAR_FILE+"] not found", file);
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull("jar file not found",jarFile);

		JarFileClassLoader cl = new JarFileClassLoader(localClassLoader);
		cl.setJar(file.getFile());
		cl.setBasePath(".");
		cl.configure(null, "");
		return TestScopeProvider.wrap(cl);
	}
}
