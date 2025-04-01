package org.frankframework.testutil;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.classloaders.AbstractClassLoader;

/**
 * ClassLoader which appends the test classpath
 */
@Log4j2
public class JunitTestClassLoaderWrapper extends AbstractClassLoader {

	private static final Path TEST_CLASSPATH;

	static {
		TEST_CLASSPATH = computeTestClassPath();
	}

	@Override
	public URL getLocalResource(String name) {
		if (name.startsWith("/")) {
			throw new IllegalArgumentException("file names should be relative, and not absolute (starting with a slash)");
		}
		Path resource = TEST_CLASSPATH.resolve(name);
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
	private static Path computeTestClassPath() {
		URL testPath = JunitTestClassLoaderWrapper.class.getResource("/");
		if (testPath == null) {
			throw new IllegalStateException("unable to locate test-classes directory");
		}
		try {
			return Path.of(testPath.toURI());
		} catch (URISyntaxException e) {
			throw new IllegalStateException("unable to resolve test-classes directory to path", e);
		}
	}

	/**
	 * Returns the normalized `test-classes` path using Unix path separators.
	 */
	public static String getTestClassesLocation() {
		return FilenameUtils.normalize(TEST_CLASSPATH.toString() + "/", true);
	}
}
