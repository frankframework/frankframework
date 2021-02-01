package nl.nn.adapterframework.testutil;

import lombok.Getter;
import nl.nn.adapterframework.core.IHasConfigurationClassLoader;

public class ClassLoaderProvider implements IHasConfigurationClassLoader {

	private @Getter ClassLoader configurationClassLoader = null;

	public ClassLoaderProvider() {
		this(Thread.currentThread().getContextClassLoader());
	}

	public ClassLoaderProvider(ClassLoader classLoader) {
		configurationClassLoader = classLoader;
	}

	public static IHasConfigurationClassLoader wrap(ClassLoader classLoader) {
		return new ClassLoaderProvider(classLoader);
	}
}
