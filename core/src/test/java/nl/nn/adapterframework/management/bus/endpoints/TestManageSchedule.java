package nl.nn.adapterframework.management.bus.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusException;
import nl.nn.adapterframework.management.bus.BusTestBase;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.ResponseMessageBase;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.SpringUtils;

public class TestManageSchedule extends BusTestBase {
	private Adapter adapter;

	@Override
	@BeforeEach
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

	@AfterEach
	@Override
	public void tearDown() throws Exception {
		if(adapter != null) {
			getConfiguration().getAdapterManager().unRegisterAdapter(adapter);
		}
		super.tearDown();
	}

	@Test
	public void getSchedules() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.SCHEDULER, BusAction.GET);
		Message<?> response = callSyncGateway(request);

		String result = response.getPayload().toString();

		String expectedJson = TestFileUtils.getTestFile("/Management/GetSchedules.json");
		MatchUtils.assertJsonEquals(expectedJson, result);
	}

	@Test
	public void manageScheduleWithoutOperation() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.SCHEDULER, BusAction.MANAGE);
		try {
			callSyncGateway(request);
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof BusException);
			assertEquals("no action specified", e.getCause().getMessage());
		}
	}

	@Test
	public void startSchedule() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.SCHEDULER, BusAction.MANAGE);
		request.setHeader("operation", "start");
		Message<?> response = callSyncGateway(request);

		String result = response.getPayload().toString();
		assertEquals(202, response.getHeaders().get(ResponseMessageBase.STATUS_KEY));
		assertEquals("no-content", result);
	}

	@Test
	public void pauzeSchedule() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.SCHEDULER, BusAction.MANAGE);
		request.setHeader("operation", "pause");
		try {
			callSyncGateway(request);
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof BusException);
			assertEquals("can only pause a started scheduler", e.getCause().getMessage());
		}

		request.setHeader("operation", "start");
		callSyncGateway(request);

		request.setHeader("operation", "pause");
		Message<?> response = callSyncGateway(request);

		String result = response.getPayload().toString();
		assertEquals(202, response.getHeaders().get(ResponseMessageBase.STATUS_KEY));
		assertEquals("no-content", result);
	}

	@Test
	public void stopSchedule() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.SCHEDULER, BusAction.MANAGE);
		request.setHeader("operation", "stop");
		Message<?> response = callSyncGateway(request);

		String result = response.getPayload().toString();
		assertEquals(202, response.getHeaders().get(ResponseMessageBase.STATUS_KEY));
		assertEquals("no-content", result);
	}
}
