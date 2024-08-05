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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventHandler extends FrankApiWebSocketBase {

	@Scheduled(fixedDelay = 60, timeUnit = TimeUnit.SECONDS)
	public void serverWarnings() {
		propagateAuthenticationContext("server-warnings");

		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.APPLICATION, BusAction.GET);
		String jsonResponse = compareAndUpdateResponse(builder);

		if (jsonResponse != null) {
			this.messagingTemplate.convertAndSend("/event/server-warnings", jsonResponse);
		}
	}

	@Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
	public void adapters() {
		propagateAuthenticationContext("adapter-info");

		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.ADAPTER, BusAction.GET);
		builder.addHeader("expanded", "all");
		String jsonResponse = compareAndUpdateResponse(builder);

		if (jsonResponse != null) {
			this.messagingTemplate.convertAndSend("/event/adapters", jsonResponse);
		}
	}

}
