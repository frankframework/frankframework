package nl.nn.adapterframework.extensions.cmis;

import org.junit.Test;

import nl.nn.adapterframework.receivers.ListenerTestBase;
import nl.nn.adapterframework.stream.Message;

public class CmisEventListenerTest extends ListenerTestBase<Message, CmisEventListener> {

	@Override
	public CmisEventListener createListener() throws Exception {
		return new CmisEventListener();
	}

	@Test
	public void testEventAttribute() throws Exception {
		listener.setEventListener("getObject");
	}
}
