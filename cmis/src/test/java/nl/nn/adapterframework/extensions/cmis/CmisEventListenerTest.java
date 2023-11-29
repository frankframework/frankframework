package nl.nn.adapterframework.extensions.cmis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.receivers.ListenerTestBase;
import nl.nn.adapterframework.stream.Message;

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
		listener.setEventListener("getObject");
	}
}
