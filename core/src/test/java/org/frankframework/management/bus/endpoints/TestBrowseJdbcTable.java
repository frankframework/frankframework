package org.frankframework.management.bus.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import org.frankframework.stream.UrlMessage;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.TestFileUtils;

public class TestBrowseJdbcTable extends BusTestBase {

	@Test
	public void testBrowseTableWithoutTablename() {
		URL url = TestFileUtils.getTestFileURL("/Management/BrowseJdbcResponseMessage.xml");
		org.frankframework.stream.Message responseXmlMessage = new UrlMessage(url);
		mockDirectQuerySenderResult("BrowseTable QuerySender", responseXmlMessage);

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
		URL url = TestFileUtils.getTestFileURL("/Management/BrowseJdbcResponseMessage.xml");
		org.frankframework.stream.Message responseXmlMessage = new UrlMessage(url);
		mockDirectQuerySenderResult("BrowseTable QuerySender", responseXmlMessage);

		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.JDBC, BusAction.FIND);
		request.setHeader("table", "testTable");
		Message<?> response = callSyncGateway(request);
		String expectedJson = TestFileUtils.getTestFile("/Management/BrowseJdbcResponseMessage.json");
		MatchUtils.assertJsonEquals(expectedJson, (String) response.getPayload());
	}

	@Test
	public void testBrowseTableMaxSmallerThenMin() {
		URL url = TestFileUtils.getTestFileURL("/Management/BrowseJdbcResponseMessage.xml");
		org.frankframework.stream.Message responseXmlMessage = new UrlMessage(url);
		mockDirectQuerySenderResult("BrowseTable QuerySender", responseXmlMessage);

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
		URL url = TestFileUtils.getTestFileURL("/Management/BrowseJdbcResponseMessage.xml");
		org.frankframework.stream.Message responseXmlMessage = new UrlMessage(url);
		mockDirectQuerySenderResult("BrowseTable QuerySender", responseXmlMessage);

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
