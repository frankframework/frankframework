package org.frankframework.console.controllers.socket;

import org.frankframework.console.controllers.SpringUnitTestLocalGateway;

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
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
class FrankApiWebSocketBaseTest {

	@Autowired
	protected SpringUnitTestLocalGateway outputGateway;

	@Autowired
	protected SimpMessagingTemplate messagingTemplate;

	@Autowired
	private MessageCacheStore messageCacheStore;

	@BeforeEach
	public void setUp() {}

	@AfterEach
	public void afterEach() {
		Mockito.reset(outputGateway);
	}

	@Test
	void compareAndUpdateResponse() {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> msg = i.getArgument(0);
			MessageHeaders headers = msg.getHeaders();
			return msg;
		});

		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.ADAPTER, BusAction.GET);
		builder.setPayload("{\"item\": \"value\"}");

	}
}
