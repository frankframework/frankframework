package org.frankframework.testutil.mock;

import org.frankframework.lifecycle.ServletManager;

public class ServletManagerMock extends ServletManager {

	public ServletManagerMock() {
		super(null);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// ignore setup
	}
}
