package org.frankframework.management.bus.endpoints;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.frankframework.configuration.Configuration;
import org.frankframework.core.Adapter;
import org.frankframework.core.PipeLine;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.receivers.JavaListener;
import org.frankframework.receivers.Receiver;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.SpringRootInitializer;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.SpringUtils;

@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
@WithMockUser(roles = { "IbisTester" })
public class TestConnectionOverview extends BusTestBase {

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		registerAdapter(getConfiguration());
	}

	protected Adapter registerAdapter(Configuration configuration) throws Exception {
		Adapter adapter = SpringUtils.createBean(configuration);
		adapter.setName("TestAdapter");

		JavaListener listener = new JavaListener();
		listener.setName("ListenerName");
		Receiver<String> receiver = new Receiver<>();
		receiver.setName("ReceiverName");
		receiver.setListener(listener);
		adapter.addReceiver(receiver);
		PipeLine pipeline = new PipeLine();
		EchoPipe pipe = SpringUtils.createBean(configuration);
		pipe.setName("EchoPipe");
		pipeline.addPipe(pipe);
		adapter.setPipeLine(pipeline);

		getConfiguration().addAdapter(adapter);
		return adapter;
	}

	@Test
	public void getAllConnections() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.CONNECTION_OVERVIEW);
		Message<?> response = callSyncGateway(request);
		String expectedJson = TestFileUtils.getTestFile("/Management/getAllConnections.json");
		MatchUtils.assertJsonEquals(expectedJson, (String) response.getPayload());
	}
}
