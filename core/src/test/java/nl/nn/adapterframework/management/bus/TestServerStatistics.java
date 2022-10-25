package nl.nn.adapterframework.management.bus;

import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

public class TestServerStatistics extends BusTestBase {

	@Test
	public void getOriginalConfigurationByName() {
		MessageBuilder request = createRequestMessage("NONE", BusTopic.STATUS, BusAction.GET);
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
}
