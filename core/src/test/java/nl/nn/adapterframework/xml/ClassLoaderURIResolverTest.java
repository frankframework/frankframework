package nl.nn.adapterframework.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;
import java.util.jar.JarFile;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.classloaders.JarFileClassLoader;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.XmlUtils;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClassLoaderURIResolverTest {

	protected final String JAR_FILE = "/Classloader/zip/classLoader-test.zip";

	
	private void testUri(ClassLoader cl, String uri, String expected) throws TransformerException {
		URL xslt = ClassUtils.getResourceURL(cl, "/ClassLoader/Xslt/root.xsl");
		assertNotNull("root.xsl not found", xslt);

		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(cl);

		Source source = resolver.resolve(uri, xslt.toString());
		assertNotNull(source);
		if (expected!=null) {
			assertEquals(expected, XmlUtils.source2String(source, false));
		} else {
			assertNotNull(XmlUtils.source2String(source, false));
		}
	}

	private void testUri(String baseType, String refType, ClassLoader cl, String base, String ref, String expected) throws TransformerException {
		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(cl);

		Source source = resolver.resolve(ref, base);
		assertNotNull(source);
		if (expected!=null) {
			assertEquals("BaseType ["+baseType+"] refType ["+refType+"]", expected, XmlUtils.source2String(source, false));
		} else {
			assertNotNull("BaseType ["+baseType+"] refType ["+refType+"]", XmlUtils.source2String(source, false));
		}
	}

	private enum BaseType { LOCAL, BYTES, FILE_SCHEME, NULL }
	private enum RefType  { SLASH, DOTDOT, SAME_FOLDER, FILE_SCHEME }
	
	private ClassLoader getClassLoader(BaseType baseType) throws ConfigurationException, IOException {
		if (baseType==BaseType.BYTES) {
			return getBytesClassLoader();
		}
		return Thread.currentThread().getContextClassLoader();
	}
	
	private String getBase(ClassLoader classLoader, BaseType baseType) throws ConfigurationException, IOException {
		URL result=null;
		switch (baseType) {
		case LOCAL:
			return "/ClassLoader/Xslt/root.xsl";
		case BYTES:
			result = ClassUtils.getResourceURL(classLoader, "/ClassLoader/Xslt/root.xsl");
			return result.toExternalForm();
		case FILE_SCHEME:
			result = ClassUtils.getResourceURL(classLoader, "/ClassLoader/Xslt/root.xsl");
			return result.toExternalForm();
		case NULL:
			return null;
		}
		return null;
	}

	private String getRef(BaseType baseType, RefType refType) {
		switch (refType) {
		case DOTDOT:
			if (baseType==BaseType.NULL) {
				return null;
			}
			return "../folder/file.xml";
		case SAME_FOLDER:
			if (baseType==BaseType.NULL) {
				return null;
			}
			return "names.xsl";
		case SLASH:
			return "/ClassLoader/folder/file.xml";
		case FILE_SCHEME:
			return ClassUtils.getResourceURL(this, "/ClassLoader/folder/file.xml").toExternalForm();
		}
		return null;
	}

	private String getExpected(BaseType baseType, RefType refType) {
		if (refType==RefType.SAME_FOLDER) {
			return null;
		}
		if (baseType==BaseType.BYTES && refType!=RefType.FILE_SCHEME) {
			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>zip:/folder/file.xml</file>";
		}
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>file:/folder/file.xml</file>";
	}

	@Test
	public void testAll() throws ConfigurationException, IOException, TransformerException {
		for(BaseType baseType:BaseType.values()) {
			ClassLoader classLoader = getClassLoader(baseType);
			String baseUrl = getBase(classLoader, baseType);
			System.out.println("BaseType ["+baseType+"] classLoader ["+classLoader+"] BaseUrl ["+baseUrl+"]");
			
			for (RefType refType: RefType.values()) {
				String ref = getRef(baseType,refType);
				String expected = getExpected(baseType,refType);
				System.out.println("BaseType ["+baseType+"] refType ["+refType+"] ref ["+ref+"] expected ["+expected+"]");
				if (ref!=null) {
					testUri(baseType.name(), refType.name(), classLoader, baseUrl, ref, expected);
				}
			}
		}
	}
	
	@Test
	public void localClass1PathRelative() throws TransformerException {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL xslt = ClassUtils.getResourceURL(localClassLoader, "/Xslt/importDocument/importLookupAbsolute1.xsl");
		assertNotNull(xslt);

		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(localClassLoader);
		Source source = resolver.resolve("lookup.xml", xslt.toString());
		assertNotNull(source);
	}

	@Test
	public void localClass2PathAbsolute() throws TransformerException {
		ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();

		URL xslt = ClassUtils.getResourceURL(localClassLoader, "/Xslt/importDocument/importLookupAbsolute1.xsl");
		assertNotNull(xslt);

		ClassLoaderURIResolver resolver = new ClassLoaderURIResolver(localClassLoader);
		Source source = resolver.resolve("/Xslt/importDocument/lookup.xml", xslt.toString());
		assertNotNull(source);
	}

	
	
	@Test
	public void bytesClassPath1Absolute() throws TransformerException, IOException, ConfigurationException {
		ClassLoader cl = getBytesClassLoader();
		testUri(cl,"/ClassLoader/folder/file.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>zip:/folder/file.xml</file>");
	}

	@Test
	public void bytesClassPath2RelativeSameFolder() throws TransformerException, IOException, ConfigurationException {
		ClassLoader cl = getBytesClassLoader();
		testUri(cl,"names.xsl",null);
	}

	@Test
	public void bytesClassPath3RelativeWithPath() throws TransformerException, IOException, ConfigurationException {
		ClassLoader cl = getBytesClassLoader();
		testUri(cl,"../folder/file.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>zip:/folder/file.xml</file>");
	}

	@Test
	public void bytesClassPath4ResourceFromLocalClasspathAbsolute() throws TransformerException, IOException, ConfigurationException {
		ClassLoader cl = getBytesClassLoader();
		testUri(cl,"/ClassLoader/folder/fileOnlyOnLocalClassPath.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>file:/folder/fileOnlyOnLocalClassPath.xml</file>");
	}

	@Test
	public void bytesClassPath5ResourceFromLocalClasspathRelative() throws TransformerException, IOException, ConfigurationException {
		ClassLoader cl = getBytesClassLoader();
		testUri(cl,"../folder/fileOnlyOnLocalClassPath.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>file:/folder/fileOnlyOnLocalClassPath.xml</file>");
	}

	@Test
	public void bytesClassPath6AbsoluteWithScheme() throws TransformerException, IOException, ConfigurationException {
		ClassLoader cl = getBytesClassLoader();
		testUri(cl,"bytesclassloader:/ClassLoader/folder/file.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>zip:/folder/file.xml</file>");
	}

	@Test
	@Ignore("GvB 2019-10-28: I think a scheme and a relative path cannot go together")
	public void bytesClassPath7RelativeSameFolderWithScheme() throws TransformerException, IOException, ConfigurationException {
		ClassLoader cl = getBytesClassLoader();
		testUri(cl,"bytesclassloader:names.xsl",null);
	}

	@Test
	@Ignore("GvB 2019-10-28: I think a scheme and a relative path cannot go together")
	public void bytesClassPath8RelativeWithPathWithScheme() throws TransformerException, IOException, ConfigurationException {
		ClassLoader cl = getBytesClassLoader();
		testUri(cl,"bytesclassloader:../folder/file.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><file>zip:/folder/file.xml</file>");
	}

	
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
//		String path = this.getClass().getResource("/ClassLoader/").toString();
//		System.err.println(path);
//		return new BasePathClassLoader(cl, path);
	}
}
