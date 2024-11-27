package org.frankframework.management.bus.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.stream.UrlMessage;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.SpringRootInitializer;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.CloseUtils;

@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
@WithMockUser(roles = { "IbisTester" })
public class TestBrowseJdbcTable extends BusTestBase {
	private org.frankframework.stream.Message inputXmlMessage;

	@BeforeEach
	public void setup() {
		URL url = TestFileUtils.getTestFileURL("/Management/BrowseJdbcTable/request.xml");
		inputXmlMessage = new UrlMessage(url);
	}

	@AfterEach
	public void teardown() {
		CloseUtils.closeSilently(inputXmlMessage);
	}

	@Test
	public void testBrowseTableWithoutTablename() {
		mockDirectQuerySenderResult("BrowseTable QuerySender", inputXmlMessage);

		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.JDBC, BusAction.FIND);
		try {
			callSyncGateway(request);
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof BusException);
			BusException be = (BusException) e.getCause();
			assertEquals("Access to table [null] not allowed", be.getMessage());
		}
	}

	@Test
	public void testBrowseTable() throws Exception {
		mockDirectQuerySenderResult("BrowseTable QuerySender", inputXmlMessage);

		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.JDBC, BusAction.FIND);
		request.setHeader("table", "testTable");
		Message<?> response = callSyncGateway(request);
		String expectedJson = TestFileUtils.getTestFile("/Management/BrowseJdbcTable/default.json");
		MatchUtils.assertJsonEquals(expectedJson, (String) response.getPayload());
	}

	@Test
	public void testBrowseTableNumberOfRowsOnly() throws Exception {
		mockDirectQuerySenderResult("BrowseTable QuerySender", inputXmlMessage);

		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.JDBC, BusAction.FIND);
		request.setHeader("numberOfRowsOnly", true);
		request.setHeader("table", "testTable");
		Message<?> response = callSyncGateway(request);
		String expectedJson = TestFileUtils.getTestFile("/Management/BrowseJdbcTable/nrOfRows.json");
		MatchUtils.assertJsonEquals(expectedJson, (String) response.getPayload());
	}

	@Test
	public void testBrowseTableOrderBy() throws Exception {
		mockDirectQuerySenderResult("BrowseTable QuerySender", inputXmlMessage);

		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.JDBC, BusAction.FIND);
		request.setHeader("order", "date");
		request.setHeader("table", "testTable");
		Message<?> response = callSyncGateway(request);
		String expectedJson = TestFileUtils.getTestFile("/Management/BrowseJdbcTable/orderBy.json");
		MatchUtils.assertJsonEquals(expectedJson, (String) response.getPayload());
	}

	@Test
	public void testBrowseTableWhere() throws Exception {
		mockDirectQuerySenderResult("BrowseTable QuerySender", inputXmlMessage);

		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.JDBC, BusAction.FIND);
		request.setHeader("where", "something=\"true\"");
		request.setHeader("table", "testTable");
		Message<?> response = callSyncGateway(request);
		String expectedJson = TestFileUtils.getTestFile("/Management/BrowseJdbcTable/where.json");
		MatchUtils.assertJsonEquals(expectedJson, (String) response.getPayload());
	}

	@Test
	public void testBrowseTableMaxSmallerThenMin() {
		mockDirectQuerySenderResult("BrowseTable QuerySender", inputXmlMessage);

		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.JDBC, BusAction.FIND);
		request.setHeader("table", "testTable");
		request.setHeader("minRow", 10);
		request.setHeader("maxRow", 1);
		try {
			callSyncGateway(request);
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof BusException);
			BusException be = (BusException) e.getCause();
			assertEquals("Rownum max must be greater than or equal to Rownum min", be.getMessage());
		}
	}

	@Test
	public void testBrowseTableMaxMoreThen100() {
		mockDirectQuerySenderResult("BrowseTable QuerySender", inputXmlMessage);

		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.JDBC, BusAction.FIND);
		request.setHeader("table", "testTable");
		request.setHeader("minRow", 1);
		request.setHeader("maxRow", 101);
		try {
			callSyncGateway(request);
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof BusException);
			BusException be = (BusException) e.getCause();
			assertEquals("Difference between Rownum max and Rownum min must be less than hundred", be.getMessage());
		}
	}
}
