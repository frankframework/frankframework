package org.frankframework.management.bus.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.frankframework.configuration.Configuration;
import org.frankframework.core.Adapter;
import org.frankframework.core.PipeLine;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.receivers.JavaListener;
import org.frankframework.receivers.Receiver;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.SpringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;

public class TestHealth extends BusTestBase {
	private Adapter adapter;

	@BeforeEach
	@Override
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
	public void tearDown() {
		if(adapter != null) {
			getConfiguration().getAdapterManager().removeAdapter(adapter);
		}
		super.tearDown();
	}

	@Test
	public void getIbisHealth() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.HEALTH);
		Message<?> response = callSyncGateway(request);

		String result = response.getPayload().toString();
		assertEquals("{\"errors\":[\"configuration[TestConfiguration] is in state[STARTING]\"],\"status\":\"SERVICE_UNAVAILABLE\"}", result);
	}

	@Test
	public void getConfigurationHealth() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.HEALTH);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
		Message<?> response = callSyncGateway(request);

		String result = response.getPayload().toString();
		assertEquals("{\"errors\":[\"adapter[TestAdapter] is in state[STOPPED]\"],\"status\":\"SERVICE_UNAVAILABLE\"}", result);
	}

	@Test
	public void getAdapterHealth() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.HEALTH);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
		request.setHeader("adapter", "TestAdapter");
		Message<?> response = callSyncGateway(request);

		String result = response.getPayload().toString();
		assertEquals("{\"errors\":[\"adapter[TestAdapter] is in state[STOPPED]\"],\"status\":\"SERVICE_UNAVAILABLE\"}", result);
	}
}
