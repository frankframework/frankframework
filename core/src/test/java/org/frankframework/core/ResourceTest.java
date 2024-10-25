package org.frankframework.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.URL;
import java.util.jar.JarFile;

import javax.xml.transform.TransformerException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import org.frankframework.configuration.classloaders.JarFileClassLoader;
import org.frankframework.testutil.TestScopeProvider;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.XmlUtils;
import org.frankframework.xml.ClassLoaderURIResolver;

public class ResourceTest {

	protected final String JAR_FILE = "/ClassLoader/zip/classLoader-test.zip";

	private final IScopeProvider testScopeProvider = new TestScopeProvider();

	private void testUri(IScopeProvider cl, String ref, String expectedContents, String expectedSystemId) throws TransformerException, SAXException, IOException {
		testUri(cl, ref, null, expectedContents, expectedSystemId);
	}

	private void testUri(IScopeProvider cl, String ref, String allowedProtocol, String expectedContents, String expectedSystemId) throws TransformerException, SAXException, IOException {
		Resource resource = Resource.getResource(cl, ref, allowedProtocol);
		assertNotNull(resource, "<null> resource: "+ref);
		if (expectedContents!=null) {
			assertEquals(expectedContents, XmlUtils.source2String(resource.asSource()));
		} else {
			assertNotNull( XmlUtils.source2String(resource.asSource()));
		}
		assertEquals(expectedSystemId,resource.getSystemId());
	}

	@Test
	public void localClassLoaderPlainRef() throws Exception {
		testUri(null, "/ClassLoader/ClassLoaderTestFile.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>/ClassLoader/ClassLoaderTestFile.xml</file>", "classpath:/ClassLoader/ClassLoaderTestFile.xml");
	}

	@Test
	public void localClassLoaderClasspathRef() throws Exception {
		testUri(null, "classpath:/ClassLoader/ClassLoaderTestFile.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>/ClassLoader/ClassLoaderTestFile.xml</file>", "classpath:/ClassLoader/ClassLoaderTestFile.xml");
	}

	@Test
	public void localClassLoaderFileRef() throws Exception {
		URL url = ClassLoaderUtils.getResourceURL(testScopeProvider, "/ClassLoader/ClassLoaderTestFile.xml");
		assertNotNull(url);
		String ref=url.toExternalForm();
		testUri(null, ref, "file", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>/ClassLoader/ClassLoaderTestFile.xml</file>", ref);
	}

	@Test
	public void bytesClassLoaderPlainRef() throws Exception {
		testUri(getBytesClassLoaderProvider(), "/ClassLoader/ClassLoaderTestFile.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>/ClassLoader/ClassLoaderTestFile.xml</file>", "classpath:/ClassLoader/ClassLoaderTestFile.xml");
	}

	@Test
	public void bytesClassLoaderClasspathRef() throws Exception {
		testUri(getBytesClassLoaderProvider(), "classpath:/ClassLoader/ClassLoaderTestFile.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>/ClassLoader/ClassLoaderTestFile.xml</file>", "classpath:/ClassLoader/ClassLoaderTestFile.xml");
	}

	@Test
	public void bytesClassLoaderFileRef() throws Exception {
		URL url = ClassLoaderUtils.getResourceURL(testScopeProvider, "/ClassLoader/ClassLoaderTestFile.xml");
		assertNotNull(url);
		String ref=url.toExternalForm();
		testUri(getBytesClassLoaderProvider(), ref, "file", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>/ClassLoader/ClassLoaderTestFile.xml</file>", ref);
	}

	@Test
	@Disabled("we do not see this as a problem for situations outside ConfigurationClassLoaded classes")
	public void testResolveOutsideParentsFolder() throws TransformerException {
		String baseResource = "/org/apache/xerces/impl/Constants.class";
		String relativeResource = "../dom/CommentImpl.class";

		URL url = this.getClass().getResource(baseResource);
		//System.out.println("url ["+url.toExternalForm()+"]");
		Resource resource = Resource.getResource(baseResource);
		//System.out.println("resource ["+resource.getSystemId()+"]");

		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(resource);
		assertNotNull(resolver.resolve(relativeResource, url.toExternalForm()));
	}

//	private ClassLoader getClassLoader(BaseType baseType) throws ConfigurationException, IOException {
//		if (baseType==BaseType.BYTES) {
//			return getBytesClassLoader();
//		}
//		return Thread.currentThread().getContextClassLoader();
//	}
//
//	private String getBase(ClassLoader classLoader, BaseType baseType) throws ConfigurationException, IOException {
//		URL result=null;
//		switch (baseType) {
//		case LOCAL:
//			return "/ClassLoader/Xslt/root.xsl";
//		case BYTES:
//			result = ClassLoaderUtils.getResourceURL(classLoader, "/ClassLoader/Xslt/root.xsl");
//			return result.toExternalForm();
//		case FILE_SCHEME:
//			result = ClassLoaderUtils.getResourceURL(classLoader, "/ClassLoader/Xslt/root.xsl");
//			return result.toExternalForm();
//		case NULL:
//			return null;
//		default:
//			throw new ConfigurationException("getBase() appears to be missing case for baseType ["+baseType+"]");
//		}
//	}
//
//	private String getRef(BaseType baseType, RefType refType) throws ConfigurationException {
//		switch (refType) {
//		case ROOT:
//			return "/ClassLoaderTestFile.xml";
//		case ABS_PATH:
//			return "/ClassLoader/ClassLoaderTestFile.xml";
//		case DOTDOT:
//			if (baseType==BaseType.NULL) {
//				return null;
//			}
//			return "../subfolder/ClassLoaderTestFile.xml";
//		case SAME_FOLDER:
//			if (baseType==BaseType.NULL) {
//				return null;
//			}
//			return "names.xsl";
//		case OVERRIDABLE:
//			return "/ClassLoader/overridablefile.xml";
//		case FILE_SCHEME:
//			return ClassLoaderUtils.getResourceURL(this, "/ClassLoader/overridablefile.xml").toExternalForm();
//		default:
//			throw new ConfigurationException("getRef() appears to be missing case for refType ["+refType+"]");
//		}
//	}
//
//	private String getExpected(BaseType baseType, RefType refType) throws ConfigurationException {
//		switch(refType) {
//		case ROOT:
//			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>/ClassLoaderTestFile.xml</file>";
//		case ABS_PATH:
//			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>/ClassLoader/ClassLoaderTestFile.xml</file>";
//		case DOTDOT:
//			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>/ClassLoader/subfolder/ClassLoaderTestFile.xml</file>";
//		case SAME_FOLDER:
//			return null;
//		case OVERRIDABLE:
//			if (baseType==BaseType.BYTES) {
//				return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>zip:/overrideablefile.xml</file>";
//			}
//			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>local:/overrideablefile.xml</file>";
//		case FILE_SCHEME:
//			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>local:/overrideablefile.xml</file>";
//		default:
//			throw new ConfigurationException("getExpected() appears to be missing case for refType ["+refType+"]");
//		}
//	}
//
//	@Test
//	public void test() throws ConfigurationException, IOException, TransformerException {
//		ClassLoader classLoader = getClassLoader(baseType);
//		String baseUrl = getBase(classLoader, baseType);
//		System.out.println("BaseType ["+baseType+"] classLoader ["+classLoader+"] BaseUrl ["+baseUrl+"]");
//
//		String ref = getRef(baseType,refType);
//		String expected = getExpected(baseType,refType);
//		System.out.println("BaseType ["+baseType+"] refType ["+refType+"] ref ["+ref+"] expected ["+expected+"]");
//		if (ref!=null) {
//			testUri(baseType.name(), refType.name(), classLoader, baseUrl, ref, expected);
//		}
//	}


	private IScopeProvider getBytesClassLoaderProvider() throws Exception {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL file = this.getClass().getResource(JAR_FILE);
		assertNotNull(file, "jar url ["+JAR_FILE+"] not found");
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull(jarFile, "jar file not found");

		JarFileClassLoader cl = new JarFileClassLoader(localClassLoader);
		cl.setJar(file.getFile());
		cl.configure(null, "");
		return TestScopeProvider.wrap(cl);
	}
}
