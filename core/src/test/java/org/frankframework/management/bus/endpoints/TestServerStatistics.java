package org.frankframework.management.bus.endpoints;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Clock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.util.TimeProvider;

public class TestServerStatistics extends BusTestBase {

	@AfterEach
	void afterEach() {
		TimeProvider.resetClock();
	}

	@Test
	public void getServerInformation() {
		TimeProvider.setClock(Clock.systemUTC());
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.APPLICATION, BusAction.GET);
		Message<?> response = callSyncGateway(request);

		String result = response.getPayload().toString();
		assertThat(result, containsString("\"fileSystem\":{")); //Object
		assertThat(result, containsString("\"framework\":{")); //Object
		assertThat(result, containsString("\"instance\":{")); //Object
		assertThat(result, containsString("\"applicationServer\":\"")); //String
		assertThat(result, containsString("\"javaVersion\":\"")); //String
		assertThat(result, containsString("\"dtap.stage\":\"")); //String
		assertThat(result, containsString("\"dtap.side\":\"")); //String
		assertThat(result, containsString("\"processMetrics\":{")); //Object
		assertThat(result, containsString("\"machineName\":\"")); //String
		assertThat(result, containsString("\"serverTimezone\":\"ETC/UTC\"")); //String
		assertThat(result, not(containsString("\"Z\""))); //String
	}

	@Test
	public void getApplicationWarnings() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.APPLICATION, BusAction.WARNINGS);
		Message<?> response = callSyncGateway(request);

		String result = response.getPayload().toString();
		assertThat(result, containsString("{\"errorStoreCount\":0")); //No errors in the IbisStore
		assertThat(result, containsString("\"totalErrorStoreCount\":0"));
		assertThat(result, containsString("\"messages\":[{\"date\":")); //Messages object with an Array with Objects
	}

	/**
	 * Method: getFileSystemTotalSpace()
	 */
	@Test
	public void testGetFileSystemTotalSpace() {
		assertNotNull(ServerStatistics.getFileSystemTotalSpace());
	}

	/**
	 * Method: getFileSystemFreeSpace()
	 */
	@Test
	public void testGetFileSystemFreeSpace() {
		assertNotNull(ServerStatistics.getFileSystemFreeSpace());
	}
}
