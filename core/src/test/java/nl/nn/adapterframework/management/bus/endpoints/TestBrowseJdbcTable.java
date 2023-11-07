package nl.nn.adapterframework.management.bus.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusException;
import nl.nn.adapterframework.management.bus.BusTestBase;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class TestBrowseJdbcTable extends BusTestBase {

	@Test
	public void testBrowseTableWithoutTablename() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/Management/BrowseJdbcResponseMessage.xml");
		nl.nn.adapterframework.stream.Message responseXmlMessage = new nl.nn.adapterframework.stream.UrlMessage(url);
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
		nl.nn.adapterframework.stream.Message responseXmlMessage = new nl.nn.adapterframework.stream.UrlMessage(url);
		mockDirectQuerySenderResult("BrowseTable QuerySender", responseXmlMessage);

		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.JDBC, BusAction.FIND);
		request.setHeader("table", "testTable");
		Message<?> response = callSyncGateway(request);
		String expectedJson = TestFileUtils.getTestFile("/Management/BrowseJdbcResponseMessage.json");
		MatchUtils.assertJsonEquals(expectedJson, (String) response.getPayload());
	}

	@Test
	public void testBrowseTableMaxSmallerThenMin() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/Management/BrowseJdbcResponseMessage.xml");
		nl.nn.adapterframework.stream.Message responseXmlMessage = new nl.nn.adapterframework.stream.UrlMessage(url);
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
	public void testBrowseTableMaxMoreThen100() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/Management/BrowseJdbcResponseMessage.xml");
		nl.nn.adapterframework.stream.Message responseXmlMessage = new nl.nn.adapterframework.stream.UrlMessage(url);
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
