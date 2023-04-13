package nl.nn.adapterframework.management.bus.endpoints;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusTestBase;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.monitoring.AdapterFilter;
import nl.nn.adapterframework.monitoring.EventType;
import nl.nn.adapterframework.monitoring.Monitor;
import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.monitoring.SourceFiltering;
import nl.nn.adapterframework.monitoring.Trigger;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.SpringUtils;

public class TestMonitoring extends BusTestBase {
	private static final String TEST_MONITOR_NAME = "TestMonitor";
	private static final String TEST_TRIGGER_EVENT_NAME = "testEvent1";
	private static final int TEST_TRIGGER_ID = 0;

	public MonitorManager getMonitorManager() {
		return getConfiguration().getBean("monitorManager", MonitorManager.class);
	}

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		createMonitor();
	}

	public void createMonitor() {
		MonitorManager manager = getMonitorManager();
		Monitor monitor = SpringUtils.createBean(getConfiguration(), Monitor.class);
		monitor.setName(TEST_MONITOR_NAME);
		monitor.setType(EventType.FUNCTIONAL);
		Trigger trigger = new Trigger();
		AdapterFilter filter = new AdapterFilter();
		filter.setAdapter("dummyAdapterName");
		filter.registerSubObject("dummySubObjectName");
		trigger.registerAdapterFilter(filter);
		trigger.setSourceFiltering(SourceFiltering.ADAPTER);
		trigger.addEventCode(TEST_TRIGGER_EVENT_NAME);
		monitor.registerTrigger(trigger);
		manager.addMonitor(monitor);
		manager.configure();
	}

	@Test
	public void getMonitors() throws Exception {
		// Arrange
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MONITORING, BusAction.GET);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);

		// Act
		Message<?> response = callSyncGateway(request);

		// Assert
		String expectedJson = TestFileUtils.getTestFile("/Management/Monitoring/getMonitors.json");
		String payload = (String) response.getPayload();
		MatchUtils.assertJsonEquals(expectedJson, payload);
	}

	@Test
	public void getMonitorsXML() throws Exception {
		// Arrange
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MONITORING, BusAction.GET);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
		request.setHeader("xml", true);

		// Act
		Message<?> response = callSyncGateway(request);

		// Assert
		String expectedXML = TestFileUtils.getTestFile("/Management/Monitoring/getMonitors.xml");
		String payload = (String) response.getPayload();
		MatchUtils.assertXmlEquals(expectedXML, payload);
	}

	@Test
	public void getMonitor() throws Exception {
		// Arrange
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MONITORING, BusAction.GET);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
		request.setHeader("monitor", TEST_MONITOR_NAME);

		// Act
		Message<?> response = callSyncGateway(request);

		// Assert
		String expectedJson = TestFileUtils.getTestFile("/Management/Monitoring/getMonitor.json");
		String payload = (String) response.getPayload();
		MatchUtils.assertJsonEquals(expectedJson, payload);
	}

	@Test
	public void getMonitorXML() throws Exception {
		// Arrange
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MONITORING, BusAction.GET);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
		request.setHeader("monitor", TEST_MONITOR_NAME);
		request.setHeader("xml", true);

		// Act
		Message<?> response = callSyncGateway(request);

		// Assert
		String expectedXML = TestFileUtils.getTestFile("/Management/Monitoring/getMonitor.xml");
		String payload = (String) response.getPayload();
		MatchUtils.assertXmlEquals(expectedXML, payload);
	}

	@Test
	public void getTrigger() throws Exception {
		// Arrange
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MONITORING, BusAction.GET);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
		request.setHeader("monitor", TEST_MONITOR_NAME);
		request.setHeader("trigger", TEST_TRIGGER_ID);

		// Act
		Message<?> response = callSyncGateway(request);

		// Assert
		String expectedJson = TestFileUtils.getTestFile("/Management/Monitoring/getTrigger.json");
		String payload = (String) response.getPayload();
		MatchUtils.assertJsonEquals(expectedJson, payload);
	}
}