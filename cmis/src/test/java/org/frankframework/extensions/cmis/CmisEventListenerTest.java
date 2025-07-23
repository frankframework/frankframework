package org.frankframework.extensions.cmis;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.receivers.ListenerTestBase;
import org.frankframework.stream.Message;

public class CmisEventListenerTest extends ListenerTestBase<Message, CmisEventListener> {

	@Override
	public CmisEventListener createListener() throws Exception {
		return new CmisEventListener();
	}

	@BeforeEach
	@Override
	public void setUp() throws Exception { //Make this test Junit5 compliant
		super.setUp();
	}

	@Test
	public void testEventAttribute() {
		assertDoesNotThrow(() -> listener.setEventListener("getObject"));
	}
}
