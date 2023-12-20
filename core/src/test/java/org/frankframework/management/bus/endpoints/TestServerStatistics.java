package org.frankframework.management.bus.endpoints;

import static org.hamcrest.MatcherAssert.assertThat;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;

public class TestServerStatistics extends BusTestBase {

	@Test
	public void getServerInformation() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.APPLICATION, BusAction.GET);
		Message<?> response = callSyncGateway(request);

		String result = response.getPayload().toString();
		assertThat(result, CoreMatchers.containsString("\"fileSystem\":{")); //Object
		assertThat(result, CoreMatchers.containsString("\"framework\":{")); //Object
		assertThat(result, CoreMatchers.containsString("\"instance\":{")); //Object
		assertThat(result, CoreMatchers.containsString("\"applicationServer\":\"")); //String
		assertThat(result, CoreMatchers.containsString("\"javaVersion\":\"")); //String
		assertThat(result, CoreMatchers.containsString("\"dtap.stage\":\"")); //String
		assertThat(result, CoreMatchers.containsString("\"dtap.side\":\"")); //String
		assertThat(result, CoreMatchers.containsString("\"processMetrics\":{")); //Object
		assertThat(result, CoreMatchers.containsString("\"machineName\":\"")); //String
	}

	@Test
	public void getApplicationWarnings() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.APPLICATION, BusAction.WARNINGS);
		Message<?> response = callSyncGateway(request);

		String result = response.getPayload().toString();
		assertThat(result, CoreMatchers.containsString("{\"errorStoreCount\":0")); //No errors in the IbisStore
		assertThat(result, CoreMatchers.containsString("\"totalErrorStoreCount\":0"));
		assertThat(result, CoreMatchers.containsString("\"messages\":[{\"date\":")); //Messages object with an Array with Objects
	}
}
