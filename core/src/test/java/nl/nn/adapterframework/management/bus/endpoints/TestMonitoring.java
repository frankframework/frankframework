package nl.nn.adapterframework.management.bus.endpoints;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTestBase;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.monitoring.AdapterFilter;
import nl.nn.adapterframework.monitoring.EventType;
import nl.nn.adapterframework.monitoring.IMonitorAdapter;
import nl.nn.adapterframework.monitoring.Monitor;
import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.monitoring.Severity;
import nl.nn.adapterframework.monitoring.SourceFiltering;
import nl.nn.adapterframework.monitoring.Trigger;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.SpringUtils;
import nl.nn.adapterframework.util.XmlBuilder;

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
		IMonitorAdapter ima = new IMonitorAdapter() {
			private @Getter @Setter String name = "mockDestination";

			@Override
			public void configure() throws ConfigurationException {
			}

			@Override
			public void fireEvent(String subSource, EventType eventType, Severity severity, String message, Throwable t) {
			}

			@Override
			public XmlBuilder toXml() {
				return new XmlBuilder("destination");
			}
		};
		manager.registerDestination(ima);
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

	@ParameterizedTest
	@ValueSource(strings = {"raise", "clear"})
	public void updateMonitorState(String state) throws Exception {
		// Arrange
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MONITORING, BusAction.MANAGE);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
		request.setHeader("monitor", TEST_MONITOR_NAME);
		request.setHeader("state", state);

		// Act
		Message<?> response = callSyncGateway(request);

		// Assert
		assertAll(
				() -> assertEquals(1, getMonitorManager().getMonitors().size()),
				() -> assertEquals("raise".equals(state), getMonitorManager().getMonitor(0).isRaised()),
				() -> assertEquals(202, BusMessageUtils.getIntHeader(response, "meta-status", 0)),
				() -> assertEquals("no-content", response.getPayload())
			);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "mockDestination")
	public void updateMonitor(String destination) throws Exception {
		// Arrange
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MONITORING, BusAction.MANAGE);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
		request.setHeader("monitor", TEST_MONITOR_NAME);
		request.setHeader("name", "new" + TEST_MONITOR_NAME);
		request.setHeader("type", EventType.TECHNICAL.name());
		request.setHeader("destinations", destination);

		// Act
		Message<?> response = callSyncGateway(request);

		// Assert
		assertAll(
				() -> assertEquals(1, getMonitorManager().getMonitors().size()),
				() -> assertEquals("new"+ TEST_MONITOR_NAME, getMonitorManager().getMonitor(0).getName()),
				() -> assertEquals(EventType.TECHNICAL, getMonitorManager().getMonitor(0).getType()),
				() -> assertEquals(202, BusMessageUtils.getIntHeader(response, "meta-status", 0)),
				() -> assertEquals("no-content", response.getPayload())
			);
		String destAsString = getMonitorManager().getMonitor(0).getDestinationsAsString();
		if(StringUtils.isBlank(destination)) {
			assertNull(destAsString);
		} else {
			assertEquals("mockDestination", destAsString);
		}
	}

	@Test
	public void deleteMonitor() throws Exception {
		// Arrange
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MONITORING, BusAction.DELETE);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
		request.setHeader("monitor", TEST_MONITOR_NAME);

		// Act
		Message<?> response = callSyncGateway(request);

		// Assert
		assertAll(
			() -> assertEquals(0, getMonitorManager().getMonitors().size()),
			() -> assertEquals(202, BusMessageUtils.getIntHeader(response, "meta-status", 0)),
			() -> assertEquals("no-content", response.getPayload())
		);
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

	@Test
	public void deleteTrigger() throws Exception {
		// Arrange
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MONITORING, BusAction.DELETE);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
		request.setHeader("monitor", TEST_MONITOR_NAME);
		request.setHeader("trigger", TEST_TRIGGER_ID);

		// Act
		Message<?> response = callSyncGateway(request);

		// Assert
		assertAll(
				() -> assertEquals(1, getMonitorManager().getMonitors().size()),
				() -> assertEquals(0, getMonitorManager().getMonitor(0).getTriggers().size()),
				() -> assertEquals(202, BusMessageUtils.getIntHeader(response, "meta-status", 0)),
				() -> assertEquals("no-content", response.getPayload())
			);
	}
}