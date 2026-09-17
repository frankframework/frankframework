package org.frankframework.util.flow;

import org.frankframework.core.IConfigurable;

public class UnloadableClass implements IConfigurable {
	static String fail = null;
	static {
		// Force an exception so the class won't load
		System.err.println(fail.length());
	}

	@Override
	public void configure() {
		// No-op
	}
}
