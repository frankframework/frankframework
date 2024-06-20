/*
   Copyright 2024 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
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
public class EventController extends FrankApiWebSocketBase {

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
	public void serverWarnings() {
		this.messagingTemplate.convertAndSend("/event/test", "processing server warnings");
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.APPLICATION, BusAction.UPDATES);

		Message<?> message = sendSyncMessageWithoutHttp(builder);
		Message<?> response = convertMessageToDiff(builder.getBusMessageName(), message);

		if (response != null) {
			this.messagingTemplate.convertAndSend("/event/server-warnings", response.getPayload());
		}
	}

	@Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
	public void adapters() {
		this.messagingTemplate.convertAndSend("/event/test", "processing adapter info");
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.ADAPTER, BusAction.UPDATES);
		builder.addHeader("expanded", "all");

		Message<?> message = sendSyncMessageWithoutHttp(builder);
		Message<?> response = convertMessageToDiff(builder.getBusMessageName(), message);

		if (response != null) {
			this.messagingTemplate.convertAndSend("/event/adapters", response.getPayload());
		}
	}

}
