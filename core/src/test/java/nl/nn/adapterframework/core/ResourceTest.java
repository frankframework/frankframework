package nl.nn.adapterframework.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;
import java.util.jar.JarFile;

import javax.xml.transform.TransformerException;

import org.junit.Test;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.classloaders.JarFileClassLoader;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.XmlUtils;

public class ResourceTest {

	protected final String JAR_FILE = "/ClassLoader/zip/classLoader-test.zip";

	private ClassLoader testClassLoader = this.getClass().getClassLoader();

	
	private void testUri(ClassLoader cl, String ref, String expectedContents, String expectedSystemId) throws TransformerException, SAXException, IOException {
		testUri(cl, ref, null, expectedContents, expectedSystemId);
	}
	
	private void testUri(ClassLoader cl, String ref, String allowedProtocol, String expectedContents, String expectedSystemId) throws TransformerException, SAXException, IOException {
		Resource resource = Resource.getResource(cl, ref, allowedProtocol);
		assertNotNull(ref,resource);
		if (expectedContents!=null) {
			assertEquals(expectedContents, XmlUtils.source2String(resource.asSource(), false));
		} else {
			assertNotNull( XmlUtils.source2String(resource.asSource(), false));
		}
		assertEquals(expectedSystemId,resource.getSystemId());
	}
	

	@Test
	public void localClassLoaderPlainRef() throws TransformerException, SAXException, IOException {
		testUri(null, "/ClassLoader/ClassLoaderTestFile.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>/ClassLoader/ClassLoaderTestFile.xml</file>", "classpath:/ClassLoader/ClassLoaderTestFile.xml");
	}
	
	@Test
	public void localClassLoaderClasspathRef() throws TransformerException, SAXException, IOException {
		testUri(null, "classpath:/ClassLoader/ClassLoaderTestFile.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>/ClassLoader/ClassLoaderTestFile.xml</file>", "classpath:/ClassLoader/ClassLoaderTestFile.xml");
	}

	@Test
	public void localClassLoaderFileRef() throws TransformerException, SAXException, IOException {
		URL url = ClassUtils.getResourceURL(testClassLoader, "/ClassLoader/ClassLoaderTestFile.xml");
		assertNotNull(url);
		String ref=url.toExternalForm();
		testUri(null, ref, "file", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>/ClassLoader/ClassLoaderTestFile.xml</file>", ref);
	}

	@Test
	public void bytesClassLoaderPlainRef() throws TransformerException, SAXException, IOException, ConfigurationException {
		ClassLoader classLoader = getBytesClassLoader();
		testUri(classLoader, "/ClassLoader/ClassLoaderTestFile.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>/ClassLoader/ClassLoaderTestFile.xml</file>", "classpath:/ClassLoader/ClassLoaderTestFile.xml");
	}
	
	@Test
	public void bytesClassLoaderClasspathRef() throws TransformerException, SAXException, IOException, ConfigurationException {
		ClassLoader classLoader = getBytesClassLoader();
		testUri(classLoader, "classpath:/ClassLoader/ClassLoaderTestFile.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>/ClassLoader/ClassLoaderTestFile.xml</file>", "classpath:/ClassLoader/ClassLoaderTestFile.xml");
	}

	@Test
	public void bytesClassLoaderFileRef() throws TransformerException, SAXException, IOException, ConfigurationException {
		URL url = ClassUtils.getResourceURL(testClassLoader, "/ClassLoader/ClassLoaderTestFile.xml");
		assertNotNull(url);
		String ref=url.toExternalForm();
		ClassLoader classLoader = getBytesClassLoader();
		testUri(classLoader, ref, "file", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>/ClassLoader/ClassLoaderTestFile.xml</file>", ref);
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
//			result = ClassUtils.getResourceURL(classLoader, "/ClassLoader/Xslt/root.xsl");
//			return result.toExternalForm();
//		case FILE_SCHEME:
//			result = ClassUtils.getResourceURL(classLoader, "/ClassLoader/Xslt/root.xsl");
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
//			return ClassUtils.getResourceURL(this, "/ClassLoader/overridablefile.xml").toExternalForm();
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

	
	private ClassLoader getBytesClassLoader() throws IOException, ConfigurationException {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL file = this.getClass().getResource(JAR_FILE);
		assertNotNull("jar url ["+JAR_FILE+"] not found", file);
		JarFile jarFile = new JarFile(file.getFile());
		assertNotNull("jar file not found",jarFile);

		JarFileClassLoader cl = new JarFileClassLoader(localClassLoader);
		cl.setJar(file.getFile());
		cl.configure(null, "");
		return cl;
	}
}
