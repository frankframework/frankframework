package org.frankframework.management.bus.endpoints;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.message.MessageBase;
import org.frankframework.monitoring.AdapterFilter;
import org.frankframework.monitoring.EventType;
import org.frankframework.monitoring.IMonitorDestination;
import org.frankframework.monitoring.ITrigger;
import org.frankframework.monitoring.ITrigger.TriggerType;
import org.frankframework.monitoring.Monitor;
import org.frankframework.monitoring.MonitorManager;
import org.frankframework.monitoring.Severity;
import org.frankframework.monitoring.SourceFiltering;
import org.frankframework.monitoring.Trigger;
import org.frankframework.monitoring.events.MonitorEvent;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.SpringRootInitializer;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.XmlBuilder;

@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
@WithMockUser(roles = { "IbisTester" })
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

	public void createMonitor() throws Exception {
		MonitorManager manager = getMonitorManager();
		Monitor monitor = SpringUtils.createBean(getConfiguration(), Monitor.class);
		monitor.setName(TEST_MONITOR_NAME);
		monitor.setType(EventType.FUNCTIONAL);
		Trigger trigger = new Trigger();
		AdapterFilter filter = new AdapterFilter();
		filter.setAdapter("dummyAdapterName");
		filter.addSubObjectText("dummySubObjectName");
		trigger.setPeriod(42);
		trigger.setThreshold(1337);
		trigger.setSeverity(Severity.HARMLESS);
		trigger.setTriggerType(TriggerType.ALARM);
		trigger.addAdapterFilter(filter);
		trigger.setSourceFiltering(SourceFiltering.ADAPTER);
		trigger.addEventCodeText(TEST_TRIGGER_EVENT_NAME);
		monitor.addTrigger(trigger);
		manager.addMonitor(monitor);
		IMonitorDestination ima = new IMonitorDestination() {
			private @Getter @Setter String name = "mockDestination";

			@Override
			public void configure() throws ConfigurationException {
				//Nothing to configure, dummy class
			}

			@Override
			public void fireEvent(String monitor, EventType eventType, Severity severity, String eventCode, MonitorEvent message) {
				//Nothing to do, dummy class
			}

			@Override
			public XmlBuilder toXml() {
				return new XmlBuilder("destination");
			}
		};
		manager.addDestination(ima);
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
	public void addMonitor() {
		// Arrange
		String requestJson = "{\"name\":\"newName\", \"type\":\"TECHNICAL\", \"destinations\":[\"mockDestination\"]}";
		MessageBuilder<String> request = createRequestMessage(requestJson, BusTopic.MONITORING, BusAction.UPLOAD);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);

		// Act
		Message<?> response = callSyncGateway(request);

		// Assert
		assertAll(
				() -> assertEquals(2, getMonitorManager().getMonitors().size()),
				() -> assertFalse(getMonitorManager().getMonitor(1).isRaised()),
				() -> assertEquals("newName", getMonitorManager().getMonitor(1).getName()),
				() -> assertEquals(EventType.TECHNICAL, getMonitorManager().getMonitor(1).getType()),
				() -> assertEquals(201, BusMessageUtils.getIntHeader(response, MessageBase.STATUS_KEY, 0)),
				() -> assertEquals("no-content", response.getPayload()),
				() -> assertEquals("mockDestination", getMonitorManager().getMonitor(1).getDestinationsAsString())
			);
	}

	@ParameterizedTest
	@ValueSource(strings = {"raise", "clear"})
	public void updateMonitorState(String state) {
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
				() -> assertEquals(202, BusMessageUtils.getIntHeader(response, MessageBase.STATUS_KEY, 0)),
				() -> assertEquals("no-content", response.getPayload())
			);
	}

	// This also indirectly tests the use of EnumUtils to parse DTO enums
	@ParameterizedTest
	@ValueSource(strings = {"TECHNICAL", "technical", "tEchNical"})
	public void updateMonitor(String type) {
		// Arrange
		String requestJson = "{\"name\":\"newName\", \"type\":\""+type+"\", \"destinations\":[\"mockDestination\"]}";
		MessageBuilder<String> request = createRequestMessage(requestJson, BusTopic.MONITORING, BusAction.MANAGE);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
		request.setHeader("monitor", TEST_MONITOR_NAME);

		// Act
		Message<?> response = callSyncGateway(request);

		// Assert
		assertAll(
				() -> assertEquals(1, getMonitorManager().getMonitors().size()),
				() -> assertEquals("newName", getMonitorManager().getMonitor(0).getName()),
				() -> assertEquals(EventType.TECHNICAL, getMonitorManager().getMonitor(0).getType()),
				() -> assertEquals(202, BusMessageUtils.getIntHeader(response, MessageBase.STATUS_KEY, 0)),
				() -> assertEquals("no-content", response.getPayload()),
				() -> assertEquals("mockDestination", getMonitorManager().getMonitor(0).getDestinationsAsString())
			);
	}

	@Test
	public void deleteMonitor() {
		// Arrange
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MONITORING, BusAction.DELETE);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
		request.setHeader("monitor", TEST_MONITOR_NAME);

		// Act
		Message<?> response = callSyncGateway(request);

		// Assert
		assertAll(
			() -> assertEquals(0, getMonitorManager().getMonitors().size()),
			() -> assertEquals(202, BusMessageUtils.getIntHeader(response, MessageBase.STATUS_KEY, 0)),
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
	public void addTrigger() {
		// Arrange
		String jsonInput = "{\"type\":\"ALARM\",\"filter\":\"NONE\",\"events\":[\"Receiver Configured\"],\"severity\":\"HARMLESS\",\"threshold\":1,\"period\":2}";
		MessageBuilder<String> request = createRequestMessage(jsonInput, BusTopic.MONITORING, BusAction.UPLOAD);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
		request.setHeader("monitor", TEST_MONITOR_NAME);

		// Act
		Message<?> response = callSyncGateway(request);

		// Assert Response
		assertAll(
				() -> assertEquals(1, getMonitorManager().getMonitors().size()),
				() -> assertEquals(2, getMonitorManager().getMonitor(0).getTriggers().size()),
				() -> assertEquals(201, BusMessageUtils.getIntHeader(response, MessageBase.STATUS_KEY, 0)),
				() -> assertEquals("no-content", response.getPayload())
			);

		// Assert Trigger
		ITrigger trigger = getMonitorManager().getMonitor(0).getTriggers().get(1);
		assertAll(
				() -> assertEquals(Severity.HARMLESS, trigger.getSeverity()),
				() -> assertEquals(2, trigger.getPeriod()),
				() -> assertEquals(1, trigger.getThreshold()),
				() -> assertEquals(TriggerType.ALARM, trigger.getTriggerType()),
				() -> assertThat(trigger.getEventCodes(), containsInAnyOrder("Receiver Configured")),
				() -> assertEquals(SourceFiltering.NONE, trigger.getSourceFiltering())
			);
	}

	@Test
	public void addMisconfiguredTrigger() {
		// Arrange
		String jsonInput = "{\"type\":\"ALARM\",\"filter\":\"NONE\",\"events\":[\"Receiver Configured\"],\"threshold\":1,\"period\":2}";
		MessageBuilder<String> request = createRequestMessage(jsonInput, BusTopic.MONITORING, BusAction.UPLOAD);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
		request.setHeader("monitor", TEST_MONITOR_NAME);

		// Before the test make sure the state is as we expect
		assertAll(
				() -> assertEquals(1, getMonitorManager().getMonitors().size()),
				() -> assertEquals(1, getMonitorManager().getMonitor(0).getTriggers().size())
		);


		// Act
		MessageHandlingException mhe = assertThrows(MessageHandlingException.class, ()-> callSyncGateway(request));

		// Assert Exception and that state has not changed
		assertInstanceOf(BusException.class, mhe.getCause());
		BusException be = (BusException) mhe.getCause();
		assertAll(
				() -> assertEquals(400, be.getStatusCode()),
				() -> assertEquals(1, getMonitorManager().getMonitors().size()),
				() -> assertEquals(1, getMonitorManager().getMonitor(0).getTriggers().size())
			);
	}

	// The request body only contains fields we want to update.
	@Test
	public void updateTriggerByAdapter() throws Exception {
		// Arrange
		String jsonInput = TestFileUtils.getTestFile("/Management/Monitoring/updateTriggerByAdapter.json");
		MessageBuilder<String> request = createRequestMessage(jsonInput, BusTopic.MONITORING, BusAction.MANAGE);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
		request.setHeader("monitor", TEST_MONITOR_NAME);
		request.setHeader("trigger", TEST_TRIGGER_ID);

		// Act
		Message<?> response = callSyncGateway(request);

		// Assert Response
		assertAll(
				() -> assertEquals(1, getMonitorManager().getMonitors().size()),
				() -> assertEquals(1, getMonitorManager().getMonitor(0).getTriggers().size()),
				() -> assertEquals(202, BusMessageUtils.getIntHeader(response, MessageBase.STATUS_KEY, 0)),
				() -> assertEquals("no-content", response.getPayload())
			);

		// Assert Trigger
		ITrigger trigger = getMonitorManager().getMonitor(0).getTriggers().get(0);
		assertAll(
				() -> assertEquals(Severity.HARMLESS, trigger.getSeverity()),
				() -> assertEquals(42, trigger.getPeriod()),
				() -> assertEquals(1337, trigger.getThreshold()),
				() -> assertEquals(TriggerType.ALARM, trigger.getTriggerType()),
				() -> assertThat(trigger.getEventCodes(), containsInAnyOrder("Pipe Exception1", "Pipe Exception2")),
				() -> assertEquals(SourceFiltering.ADAPTER, trigger.getSourceFiltering()),
				() -> assertThat(trigger.getAdapterFilters().keySet(), containsInAnyOrder("adapter1", "adapter2")),
				() -> assertEquals(0, trigger.getAdapterFilters().get("adapter1").getSubObjectList().size()),
				() -> assertEquals(0, trigger.getAdapterFilters().get("adapter2").getSubObjectList().size())
			);
	}

	// The request body contains all fields
	@Test
	public void updateTriggerBySource() throws Exception {
		// Arrange
		String jsonInput = TestFileUtils.getTestFile("/Management/Monitoring/updateTriggerBySource.json");
		MessageBuilder<String> request = createRequestMessage(jsonInput, BusTopic.MONITORING, BusAction.MANAGE);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
		request.setHeader("monitor", TEST_MONITOR_NAME);
		request.setHeader("trigger", TEST_TRIGGER_ID);

		// Act
		Message<?> response = callSyncGateway(request);

		// Assert Response
		assertAll(
				() -> assertEquals(1, getMonitorManager().getMonitors().size()),
				() -> assertEquals(1, getMonitorManager().getMonitor(0).getTriggers().size()),
				() -> assertEquals(202, BusMessageUtils.getIntHeader(response, MessageBase.STATUS_KEY, 0)),
				() -> assertEquals("no-content", response.getPayload())
			);

		// Assert Trigger
		ITrigger trigger = getMonitorManager().getMonitor(0).getTriggers().get(0);
		assertAll(
				() -> assertEquals(Severity.CRITICAL, trigger.getSeverity()),
				() -> assertEquals(3600, trigger.getPeriod()),
				() -> assertEquals(10, trigger.getThreshold()),
				() -> assertEquals(TriggerType.ALARM, trigger.getTriggerType()),
				() -> assertThat(trigger.getEventCodes(), containsInAnyOrder("Sender Timeout")),
				() -> assertEquals(SourceFiltering.SOURCE, trigger.getSourceFiltering()),
				() -> assertThat(trigger.getAdapterFilters().keySet(), containsInAnyOrder("adapter1", "adapter2")),
				() -> assertEquals(1, trigger.getAdapterFilters().get("adapter1").getSubObjectList().size()),
				() -> assertEquals(1, trigger.getAdapterFilters().get("adapter2").getSubObjectList().size())
			);
	}

	// The request body contains filter:NONE as well as filters
	@Test
	public void updateTriggerNoFilter() throws Exception {
		// Arrange
		String jsonInput = TestFileUtils.getTestFile("/Management/Monitoring/updateTriggerNoFilter.json");
		MessageBuilder<String> request = createRequestMessage(jsonInput, BusTopic.MONITORING, BusAction.MANAGE);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
		request.setHeader("monitor", TEST_MONITOR_NAME);
		request.setHeader("trigger", TEST_TRIGGER_ID);

		// Act
		Message<?> response = callSyncGateway(request);

		// Assert Response
		assertAll(
				() -> assertEquals(1, getMonitorManager().getMonitors().size()),
				() -> assertEquals(1, getMonitorManager().getMonitor(0).getTriggers().size()),
				() -> assertEquals(202, BusMessageUtils.getIntHeader(response, MessageBase.STATUS_KEY, 0)),
				() -> assertEquals("no-content", response.getPayload())
			);

		// Assert Trigger
		ITrigger trigger = getMonitorManager().getMonitor(0).getTriggers().get(0);
		assertAll(
				() -> assertEquals(Severity.CRITICAL, trigger.getSeverity()),
				() -> assertEquals(3600, trigger.getPeriod()),
				() -> assertEquals(1, trigger.getThreshold()),
				() -> assertEquals(TriggerType.ALARM, trigger.getTriggerType()),
				() -> assertThat(trigger.getEventCodes(), containsInAnyOrder("Pipe Exception", "Sender Timeout")),
				() -> assertEquals(SourceFiltering.NONE, trigger.getSourceFiltering()),
				() -> assertEquals(0, trigger.getAdapterFilters().size())
			);
	}

	// The request body contains invalid, but parsable JSON
	@Test
	public void testInvalidJsonWhenUpdatingTrigger() throws Exception {
		// Arrange
		String jsonInput = TestFileUtils.getTestFile("/Management/Monitoring/updateTriggerInvalidJson.json");
		MessageBuilder<String> request = createRequestMessage(jsonInput, BusTopic.MONITORING, BusAction.MANAGE);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
		request.setHeader("monitor", TEST_MONITOR_NAME);
		request.setHeader("trigger", TEST_TRIGGER_ID);

		// Act
		MessageHandlingException e = assertThrows(MessageHandlingException.class, () -> callSyncGateway(request));

		// Assert
		assertTrue(e.getCause() instanceof BusException);
		String exception = "unable to convert payload: (InvalidFormatException) Cannot deserialize value of type `java.lang.Integer` from String \"no-int\"";
		assertThat(e.getCause().getMessage(), Matchers.startsWith(exception));
	}

	@Test
	public void deleteTrigger() {
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
				() -> assertEquals(202, BusMessageUtils.getIntHeader(response, MessageBase.STATUS_KEY, 0)),
				() -> assertEquals("no-content", response.getPayload())
			);
	}
}
