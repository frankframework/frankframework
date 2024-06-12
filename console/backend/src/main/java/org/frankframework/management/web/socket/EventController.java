package org.frankframework.management.web.socket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class EventController {

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@PostMapping(value = "/event/push", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> push(@RequestBody String content){
		this.messagingTemplate.convertAndSend("/event/test", content);
		return ResponseEntity.ok().build();
	}

}
