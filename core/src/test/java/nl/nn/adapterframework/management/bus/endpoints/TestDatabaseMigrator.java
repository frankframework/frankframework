package nl.nn.adapterframework.management.bus.endpoints;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.InputStream;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusTestBase;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.ResponseMessage;
import nl.nn.adapterframework.webcontrol.api.FrankApiBase;
import nl.nn.credentialprovider.util.Misc;

public class TestDatabaseMigrator extends BusTestBase {

	@Test
	public void downloadMigrationScript() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.JDBC_MIGRATION, BusAction.DOWNLOAD);
		request.setHeader(FrankApiBase.HEADER_CONFIGURATION_NAME_KEY, getConfiguration().getName());
		Message<?> response = callSyncGateway(request);
		assertEquals("application/xml", response.getHeaders().get(ResponseMessage.MIMETYPE_KEY));
		InputStream resource = (InputStream) response.getPayload();
		String payload = Misc.streamToString(resource);

		assertThat(payload, Matchers.startsWith("<databaseChangeLog"));
	}
}
