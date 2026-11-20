package org.frankframework.testutil;

import java.net.URL;

import org.jspecify.annotations.NonNull;

import org.frankframework.configuration.ClassLoaderException;
import org.frankframework.configuration.classloaders.AbstractClassLoader;

public class NullClassLoader extends AbstractClassLoader {

	public NullClassLoader() {
		// NO OP
	}

	public NullClassLoader(ClassLoader parent) {
		super(parent);
	}

	@Override
	public void afterPropertiesSet() throws ClassLoaderException {
		if("ConfigWithNullClassLoader".equals(getConfigurationName())) {
			setReportLevel("INFO");
			throw new ClassLoaderException("test-exception");
		}

		super.afterPropertiesSet();
	}

	@Override
	public URL getLocalResource(@NonNull String name) {
		if("SpringConfigurationContext.xml".equals(name)) {
			throw new IllegalStateException("unable to instantiate this Spring context!");
		}
		return null;
	}

}
