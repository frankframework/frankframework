package org.frankframework.testutil;

import java.net.URL;

import org.frankframework.configuration.classloaders.AbstractClassLoader;

/**
 * ClassLoader which appends the test classpath
 */
public class JunitTestClassLoaderWrapper extends AbstractClassLoader {

	@Override
	public URL getLocalResource(String name) {
		return JunitTestClassLoaderWrapper.class.getResource(name); //test classpath
	}
}
