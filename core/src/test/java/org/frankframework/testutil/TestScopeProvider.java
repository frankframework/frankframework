package org.frankframework.testutil;

import lombok.Getter;

import org.frankframework.core.IScopeProvider;

public class TestScopeProvider implements IScopeProvider {

	private @Getter ClassLoader configurationClassLoader = null;

	public TestScopeProvider() {
		this(Thread.currentThread().getContextClassLoader());
	}

	public TestScopeProvider(ClassLoader classLoader) {
		configurationClassLoader = classLoader;
	}

	public static IScopeProvider wrap(ClassLoader classLoader) {
		return new TestScopeProvider(classLoader);
	}
}
