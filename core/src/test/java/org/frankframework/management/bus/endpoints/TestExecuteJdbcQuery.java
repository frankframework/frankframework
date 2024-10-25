package org.frankframework.management.bus.endpoints;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.stream.UrlMessage;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.SpringRootInitializer;
import org.frankframework.testutil.TestAssertions;
import org.frankframework.testutil.TestFileUtils;

@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
@WithMockUser(roles = { "IbisTester" })
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
		org.frankframework.stream.Message responseXmlMessage = new UrlMessage(url);
		mockDirectQuerySenderResult("ExecuteJdbc QuerySender", responseXmlMessage);

		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.JDBC, BusAction.MANAGE);
		request.setHeader("table", "testTable");

		// Act / Assert - Test without resultType (defaults to XML)
		String expectedXml = TestFileUtils.getTestFile("/Management/ExecuteJdbcQueryMessage.xml");
		MatchUtils.assertXmlEquals("XML Mismatch", expectedXml, (String) callSyncGateway(request).getPayload());

		// Arrange - create new open Message object.
		responseXmlMessage = new UrlMessage(url);
		mockDirectQuerySenderResult("ExecuteJdbc QuerySender", responseXmlMessage);

		// Act / Assert - Test with JSON resultType
		request.setHeader("resultType", "json");
		String expectedJson = TestFileUtils.getTestFile("/Management/ExecuteJdbcQueryMessage.json");
		MatchUtils.assertJsonEquals("JSON Mismatch", expectedJson, (String) callSyncGateway(request).getPayload());

		// Arrange - create new open Message object.
		responseXmlMessage = new UrlMessage(url);
		mockDirectQuerySenderResult("ExecuteJdbc QuerySender", responseXmlMessage);

		// Act / Assert - Test with CSV resultType
		request.setHeader("resultType", "csv");
		String expectedCsv = TestFileUtils.getTestFile("/Management/ExecuteJdbcQueryMessage.csv");
		TestAssertions.assertEqualsIgnoreCRLF(expectedCsv, (String) callSyncGateway(request).getPayload(), "CSV Mismatch");
	}
}
