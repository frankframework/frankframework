package nl.nn.adapterframework.frankdoc.doclet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.sun.javadoc.ClassDoc;

import nl.nn.adapterframework.frankdoc.doclet.classdocs.EasyDoclet;

final class TestUtil {
	private static final Properties BUILD_PROPERTIES = new TestUtil().loadBuildProperties();
	private static final File TEST_SOURCE_DIRECTORY = new File(BUILD_PROPERTIES.getProperty("testSourceDirectory"));

	private TestUtil() {
	}

	static FrankMethod getDeclaredMethodOf(FrankClass clazz, String methodName) {
		FrankMethod[] methods = clazz.getDeclaredMethods();
		for(FrankMethod m: methods) {
			if(m.getName().equals(methodName)) {
				return m;
			}
		}
		return null;
	}

	public static ClassDoc[] getClassDocs(String ...packages) {
		System.out.println("System property java.home: " + System.getProperty("java.home"));
		EasyDoclet doclet = new EasyDoclet(TEST_SOURCE_DIRECTORY, packages);
		return doclet.getRootDoc().classes();
	}

	private Properties loadBuildProperties() {
		try {
			Properties result = new Properties();
			InputStream is = getClass().getClassLoader().getResource("build.properties").openStream();
			result.load(is);
			return result;
		} catch(IOException e) {
			throw new RuntimeException("Cannot load build.properties", e);
		}
	}
}
