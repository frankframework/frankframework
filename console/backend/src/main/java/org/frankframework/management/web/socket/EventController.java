package org.frankframework.management.web.socket;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.web.FrankApiBase;
import org.frankframework.management.web.RequestMessageBuilder;
import org.frankframework.util.MessageCache;
import org.frankframework.util.MessageCacheStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.concurrent.TimeUnit;

@Controller
public class EventController extends FrankApiBase {

	private final SimpMessagingTemplate messagingTemplate;
	private final MessageCacheStore messageCacheStore;

	@Autowired
	public EventController(SimpMessagingTemplate messagingTemplate, MessageCacheStore messageCacheStore) {
		this.messagingTemplate = messagingTemplate;
		this.messageCacheStore = messageCacheStore;
	}

	@PostMapping(value = "/event/push", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> push(@RequestBody String content){
		this.messagingTemplate.convertAndSend("/event/test", content);
		return ResponseEntity.ok().build();
	}

	@Scheduled(fixedDelay = 60, timeUnit = TimeUnit.SECONDS)
	public void serverWarnings() {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.APPLICATION, BusAction.UPDATES);
//		Message<?> response = sendSyncMessageWithoutHttp(RequestMessageBuilder.create(this, BusTopic.APPLICATION, BusAction.UPDATES));
		Message<?> response = messageCacheStore.getCachedOrLatest("server-warnings", builder, this);
		this.messagingTemplate.convertAndSend("/event/server-warnings", response.getPayload());
	}

	@Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
	public void adapters() {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.ADAPTER, BusAction.UPDATES);
		builder.addHeader("expanded", "all");
//		Message<?> response = sendSyncMessageWithoutHttp(builder);
		Message<?> response = messageCacheStore.getCachedOrLatest("adapters", builder, this);
		this.messagingTemplate.convertAndSend("/event/adapters", response.getPayload());
	}

}
