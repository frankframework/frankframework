package org.frankframework.management.web.socket;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.web.FrankApiBase;
import org.frankframework.management.web.RequestMessageBuilder;
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

	@Autowired
	public EventController(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	@PostMapping(value = "/event/push", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> push(@RequestBody String content){
		this.messagingTemplate.convertAndSend("/event/test", content);
		return ResponseEntity.ok().build();
	}

	@Scheduled(fixedDelay = 60, timeUnit = TimeUnit.SECONDS)
	public void ConfigurationWarnings() {
		Message<?> response = sendSyncMessageWithoutHttp(RequestMessageBuilder.create(this, BusTopic.APPLICATION, BusAction.WARNINGS));
		this.messagingTemplate.convertAndSend("/event/server-warnings", response.getPayload());
	}

}
