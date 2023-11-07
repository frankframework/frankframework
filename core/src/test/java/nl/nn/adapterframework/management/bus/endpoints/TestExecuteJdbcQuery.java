package nl.nn.adapterframework.management.bus.endpoints;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusTestBase;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class TestExecuteJdbcQuery extends BusTestBase {

	@Test
	public void getDatasourcesTest() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.JDBC, BusAction.GET);
		Message<?> response = callSyncGateway(request);
		String expectedJson = TestFileUtils.getTestFile("/Management/GetDatasources.json");
		MatchUtils.assertJsonEquals(expectedJson, (String) response.getPayload());
	}

	@Test
	public void testExecuteQuery() throws Exception {
		// Arrange
		URL url = TestFileUtils.getTestFileURL("/Management/ExecuteJdbcQueryMessage.xml");
		nl.nn.adapterframework.stream.Message responseXmlMessage = new nl.nn.adapterframework.stream.UrlMessage(url);
		mockDirectQuerySenderResult("ExecuteJdbc QuerySender", responseXmlMessage);

		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.JDBC, BusAction.MANAGE);
		request.setHeader("table", "testTable");

		// Act / Assert - Test without resultType (defaults to XML)
		String expectedXml = TestFileUtils.getTestFile("/Management/ExecuteJdbcQueryMessage.xml");
		MatchUtils.assertXmlEquals("XML Mismatch", expectedXml, (String) callSyncGateway(request).getPayload());

		// Arrange - create new open Message object.
		responseXmlMessage = new nl.nn.adapterframework.stream.UrlMessage(url);
		mockDirectQuerySenderResult("ExecuteJdbc QuerySender", responseXmlMessage);

		// Act / Assert - Test with JSON resultType
		request.setHeader("resultType", "json");
		String expectedJson = TestFileUtils.getTestFile("/Management/ExecuteJdbcQueryMessage.json");
		MatchUtils.assertJsonEquals("JSON Mismatch", expectedJson, (String) callSyncGateway(request).getPayload());

		// Arrange - create new open Message object.
		responseXmlMessage = new nl.nn.adapterframework.stream.UrlMessage(url);
		mockDirectQuerySenderResult("ExecuteJdbc QuerySender", responseXmlMessage);

		// Act / Assert - Test with CSV resultType
		request.setHeader("resultType", "csv");
		String expectedCsv = TestFileUtils.getTestFile("/Management/ExecuteJdbcQueryMessage.csv");
		TestAssertions.assertEqualsIgnoreCRLF(expectedCsv, (String) callSyncGateway(request).getPayload(), "CSV Mismatch");
	}
}
