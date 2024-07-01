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

import java.util.concurrent.TimeUnit;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.web.RequestMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventHandler extends FrankApiWebSocketBase {

	@Scheduled(fixedDelay = 60, timeUnit = TimeUnit.SECONDS)
	public void serverWarnings() {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.APPLICATION, BusAction.UPDATES);

		Message<?> message = sendSyncMessageWithoutHttp(builder);
		this.messagingTemplate.convertAndSend("/event/server-warnings", message.getPayload());
	}

	@Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
	public void adapters() {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.ADAPTER, BusAction.UPDATES);
		builder.addHeader("expanded", "all");

		Message<?> message = sendSyncMessageWithoutHttp(builder);
		Message<?> response = convertMessageToDiff(builder.getBusMessageName(), message);

		if (!response.getPayload().equals("{}")) {
			this.messagingTemplate.convertAndSend("/event/adapters", response.getPayload());
		}
	}

}
