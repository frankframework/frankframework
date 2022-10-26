package nl.nn.adapterframework.management.bus;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.util.SpringUtils;

public class TestHealth extends BusTestBase {
	private Adapter adapter;

	@Before
	public void setUp() throws Exception {
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

	@After
	public void tearDown() {
		if(adapter != null) {
			getConfiguration().getAdapterManager().unRegisterAdapter(adapter);
		}
	}

	@Test
	public void getIbisHealth() {
		MessageBuilder request = createRequestMessage("NONE", BusTopic.HEALTH);
		Message<?> response = callSyncGateway(request);

		String result = response.getPayload().toString();
		assertEquals("{\"errors\":[\"configuration[TestConfiguration] is in state[STARTING]\",\"adapter[TestAdapter] is in state[STOPPED]\"],\"status\":\"SERVICE_UNAVAILABLE\"}", result);
	}

	@Test
	public void getConfigurationHealth() {
		MessageBuilder request = createRequestMessage("NONE", BusTopic.HEALTH);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
		Message<?> response = callSyncGateway(request);

		String result = response.getPayload().toString();
		assertEquals("{\"errors\":[\"adapter[TestAdapter] is in state[STOPPED]\"],\"status\":\"SERVICE_UNAVAILABLE\"}", result);
	}
}
