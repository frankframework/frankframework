package nl.nn.adapterframework.jdbc.migration;

import java.net.URL;

import nl.nn.adapterframework.configuration.classloaders.ClassLoaderBase;

/**
 * ClassLoader which appends the test classpath
 */
public class JunitTestClassLoaderWrapper extends ClassLoaderBase {

	@Override
	public URL getLocalResource(String name) {
		return JunitTestClassLoaderWrapper.class.getResource(name); //test classpath
	}
}
