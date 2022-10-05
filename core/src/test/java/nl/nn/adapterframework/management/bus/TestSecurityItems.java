package nl.nn.adapterframework.management.bus;

import org.junit.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

public class TestSecurityItems extends BusTestBase {

	@Test
	public void getSecurityItems() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.SECURITY_ITEMS);
		request.setHeader("configuration", "testConfiguration");
		Message<?> response = callSyncGateway(request);
		System.out.println(response);
	}
}
