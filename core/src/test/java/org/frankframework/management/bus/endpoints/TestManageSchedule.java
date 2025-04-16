package org.frankframework.management.bus.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.springframework.messaging.Message;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.frankframework.configuration.Configuration;
import org.frankframework.core.Adapter;
import org.frankframework.core.PipeLine;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.message.MessageBase;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.receivers.JavaListener;
import org.frankframework.receivers.Receiver;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.SpringRootInitializer;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.SpringUtils;

@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
@WithMockUser(roles = { "IbisTester" })
public class TestManageSchedule extends BusTestBase {

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		registerAdapter(getConfiguration());
		getConfiguration().getBean(Scheduler.class, "scheduler").standby();
	}

	protected Adapter registerAdapter(Configuration configuration) throws Exception {
		Adapter adapter = SpringUtils.createBean(configuration, Adapter.class);
		adapter.setName("TestAdapter");

		JavaListener<String> listener = new JavaListener<>();
		listener.setName("ListenerName");
		Receiver<String> receiver = new Receiver<>();
		receiver.setName("ReceiverName");
		receiver.setListener(listener);
		adapter.addReceiver(receiver);
		PipeLine pipeline = new PipeLine();
		EchoPipe pipe = SpringUtils.createBean(configuration, EchoPipe.class);
		pipe.setName("EchoPipe");
		pipeline.addPipe(pipe);
		adapter.setPipeLine(pipeline);

		getConfiguration().addAdapter(adapter);
		return adapter;
	}

	@Test
	public void getSchedules() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.SCHEDULER, BusAction.GET);
		Message<?> response = callSyncGateway(request);

		String rawResult = response.getPayload().toString();
		Pattern pattern = Pattern.compile("(\"runningSince\":)([0-9]*)");
		Matcher matcher = pattern.matcher(rawResult);
		matcher.find();
		String time = matcher.group(2);
		String result = rawResult.replace(time, "0"); // Replace the time string with `0` for easy assertions.

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
		assertEquals(202, BusMessageUtils.getIntHeader(response, MessageBase.STATUS_KEY, 0));
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
		assertEquals(202, BusMessageUtils.getIntHeader(response, MessageBase.STATUS_KEY, 0));
		assertEquals("no-content", result);
	}

	@Test
	public void stopSchedule() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.SCHEDULER, BusAction.MANAGE);
		request.setHeader("operation", "stop");
		Message<?> response = callSyncGateway(request);

		String result = response.getPayload().toString();
		assertEquals(202, BusMessageUtils.getIntHeader(response, MessageBase.STATUS_KEY, 0));
		assertEquals("no-content", result);
	}
}
