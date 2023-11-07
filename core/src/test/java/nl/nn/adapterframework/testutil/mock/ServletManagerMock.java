package nl.nn.adapterframework.testutil.mock;

import nl.nn.adapterframework.lifecycle.ServletManager;

public class ServletManagerMock extends ServletManager {

	public ServletManagerMock() {
		super(null);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// ignore setup
	}
}
