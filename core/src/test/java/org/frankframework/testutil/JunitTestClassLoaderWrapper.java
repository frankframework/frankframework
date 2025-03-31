package org.frankframework.testutil;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.classloaders.AbstractClassLoader;

/**
 * ClassLoader which appends the test classpath
 */
@Log4j2
public class JunitTestClassLoaderWrapper extends AbstractClassLoader {

	private static final String TEST_CLASSPATH;

	static {
		TEST_CLASSPATH = computeTestClassPath();
	}

	@Override
	public URL getLocalResource(String name) {
		Path resource = Path.of(TEST_CLASSPATH).resolve(name);
		if (!Files.exists(resource)) {
			return null;
		}

		try {
			return resource.toUri().toURL();
		} catch (MalformedURLException e) {
			log.error("found resource [{}] but was unable to convert it to a URL", resource, e);
			return null;
		}
	}

	/**
	 * In order to test the DirectoryClassloader we need the absolute path of where it can find it's configuration(s)
	 * @return the path to the mvn generated test-classes folder
	 */
	private static String computeTestClassPath() {
		String file = "test1.xml";
		String testPath = JunitTestClassLoaderWrapper.class.getResource("/"+file).getPath();
		int start = 0;
		if (testPath.startsWith("/")) {
			start++;
		}
		testPath = testPath.substring(start, testPath.indexOf(file));
		return testPath;
	}

	public static String getTestClassesLocation() {
		return TEST_CLASSPATH;
	}
}
