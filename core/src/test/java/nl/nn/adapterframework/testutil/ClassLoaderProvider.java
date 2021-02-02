package nl.nn.adapterframework.testutil;

import lombok.Getter;
import nl.nn.adapterframework.core.IScopeProvider;

public class ClassLoaderProvider implements IScopeProvider {

	private @Getter ClassLoader configurationClassLoader = null;

	public ClassLoaderProvider() {
		this(Thread.currentThread().getContextClassLoader());
	}

	public ClassLoaderProvider(ClassLoader classLoader) {
		configurationClassLoader = classLoader;
	}

	public static IScopeProvider wrap(ClassLoader classLoader) {
		return new ClassLoaderProvider(classLoader);
	}
}
