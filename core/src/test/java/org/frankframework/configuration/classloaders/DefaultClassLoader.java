package org.frankframework.configuration.classloaders;

import java.net.URL;

import org.jspecify.annotations.Nullable;

/**
 * Depending on the JKD this grabs the default ClassLoader
 *
 * @author Niels Meijer
 *
 */
public class DefaultClassLoader extends AbstractClassLoader {

	public DefaultClassLoader() {
		super();
	}

	public DefaultClassLoader(ClassLoader parent) {
		super(parent);
	}

	@Override
	protected @Nullable URL getLocalResource(@Nullable String name) {
		return null;
	}
}
