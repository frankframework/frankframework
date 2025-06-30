package org.frankframework.console.controllers.socket;

import org.frankframework.console.controllers.SpringUnitTestLocalGateway;

import org.frankframework.console.controllers.WebTestConfiguration;
import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { WebTestConfiguration.class, FrankApiWebSocketBase.class})
class FrankApiWebSocketBaseTest {

	FrankApiWebSocketBase webSocketBase;

	@Autowired
	protected SpringUnitTestLocalGateway outboundGateway;

	@Autowired
	private MessageCacheStore messageCacheStore;

	@BeforeEach
	public void setUp() {
		webSocketBase = Mockito.spy(new FrankApiWebSocketBase());
		webSocketBase.gateway = outboundGateway;
		webSocketBase.messageCacheStore = messageCacheStore;
	}

	@AfterEach
	public void afterEach() {
		Mockito.reset(outboundGateway);
		messageCacheStore.empty();
	}

	@Test
	@SuppressWarnings("unchecked")
	void compareAndUpdateResponseWithDiff() {
		String payload = "{\"item\": \"value\", \"status\": 1}";
		String payload2 = "{\"item\": \"value\", \"status\": 2}";
		String expectedResult = "{\"status\":2}";

		Mockito.when(outboundGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> i.getArgument(0));

		RequestMessageBuilder builder;

		builder = RequestMessageBuilder.create(BusTopic.ADAPTER, BusAction.GET);
		builder.setPayload(payload);
		String result = webSocketBase.compareAndUpdateResponse(builder, null, null);

		builder = RequestMessageBuilder.create(BusTopic.ADAPTER, BusAction.GET);
		builder.setPayload(payload2);
		String result2 = webSocketBase.compareAndUpdateResponse(builder, null, null);

		assertEquals(payload, result);
		assertEquals(expectedResult, result2);
	}

	@Test
	@SuppressWarnings("unchecked")
	void compareAndUpdateResponseWithoutDiff() {
		String payload = "{\"item\": \"value\", \"status\": 1}";

		Mockito.when(outboundGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> i.getArgument(0));

		RequestMessageBuilder builder;

		builder = RequestMessageBuilder.create(BusTopic.ADAPTER, BusAction.GET);
		builder.setPayload(payload);
		String result = webSocketBase.compareAndUpdateResponse(builder, null, null);

		builder = RequestMessageBuilder.create(BusTopic.ADAPTER, BusAction.GET);
		builder.setPayload(payload);
		String result2 = webSocketBase.compareAndUpdateResponse(builder, null, null);

		assertEquals(payload, result);
		assertNull(result2);
	}
}
