package nl.nn.adapterframework.management.bus.endpoints;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.management.bus.BusTestBase;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.SpringUtils;

public class TestConnectionOverview extends BusTestBase {
	private Adapter adapter;

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		adapter = registerAdapter(getConfiguration());
	}

	protected Adapter registerAdapter(Configuration configuration) throws Exception {
		Adapter adapter = SpringUtils.createBean(configuration, Adapter.class);
		adapter.setName("TestAdapter");

		JavaListener listener = new JavaListener();
		listener.setName("ListenerName");
		Receiver<String> receiver = new Receiver<>();
		receiver.setName("ReceiverName");
		receiver.setListener(listener);
		adapter.registerReceiver(receiver);
		PipeLine pipeline = new PipeLine();
		EchoPipe pipe = SpringUtils.createBean(configuration, EchoPipe.class);
		pipe.setName("EchoPipe");
		pipeline.addPipe(pipe);
		adapter.setPipeLine(pipeline);

		getConfiguration().registerAdapter(adapter);
		return adapter;
	}

	@AfterEach
	@Override
	public void tearDown() throws Exception {
		if(adapter != null) {
			getConfiguration().getAdapterManager().unRegisterAdapter(adapter);
		}
		super.tearDown();
	}

	@Test
	public void getAllConnections() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.CONNECTION_OVERVIEW);
		Message<?> response = callSyncGateway(request);
		String expectedJson = TestFileUtils.getTestFile("/Management/getAllConnections.json");
		MatchUtils.assertJsonEquals(expectedJson, (String) response.getPayload());
	}
}
