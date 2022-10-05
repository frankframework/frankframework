package nl.nn.adapterframework.management.bus;

import org.junit.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.testutil.FixedQuerySenderMock.ResultSetBuilder;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class TestSecurityItems extends BusTestBase {

	@Test
	public void getSecurityItems() throws Exception {
		mockQuery("select datasource from database", ResultSetBuilder.create().build());

		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.SECURITY_ITEMS);
		request.setHeader("configuration", "testConfiguration");
		Message<?> response = callSyncGateway(request);
		String expectedJson = TestFileUtils.getTestFile("/Management/securityItemsResponse.json");
		MatchUtils.assertJsonEquals(expectedJson, (String) response.getPayload());
	}
}
